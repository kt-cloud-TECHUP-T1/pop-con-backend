// 테스트용 팝업 데이터 통합 초기화 서비스
package com.t1.popcon.auction.service;

import com.t1.popcon.auction.bid.client.DrawServiceClient;
import com.t1.popcon.auction.bid.client.TicketServiceClient;
import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.bid.repository.BidRepository;
import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopupResetService {

    private final AuctionRepository auctionRepository;
    private final AuctionOptionRepository auctionOptionRepository;
    private final BidRepository bidRepository;
    private final BidRedisRepository bidRedisRepository;
    private final AuctionQueueResetService auctionQueueResetService;
    private final DrawServiceClient drawServiceClient;
    private final TicketServiceClient ticketServiceClient;
    private final StringRedisTemplate redisTemplate;

    /**
     * popupId 기준 DB 데이터 초기화 (steps 1–4)
     * 1. 티켓 삭제 — ticket-service Feign, afterCommit 훅으로 실행
     * 2. 입찰 내역 하드 딜리트
     * 3. AuctionOption.remainingStock 원복
     * 4. Auction.status/soldAt 원복
     * Redis 및 드로우 초기화는 resetRedisAndDraw() 에서 처리
     */
    @Transactional
    public void reset(Long popupId) {
        log.info(">>>> [TestReset] popupId={} 통합 초기화 시작", popupId);

        Auction auction = auctionRepository.findByPopupId(popupId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));
        long auctionId = auction.getId();

        // 1. 티켓 삭제 (Feign - ticket-service) — 트랜잭션 커밋 후 실행하여 외부 호출이 롤백에 영향받지 않도록 처리
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ticketServiceClient.deleteTicketsByPopupId(popupId);
                log.info(">>>> [TestReset] popupId={} 티켓 삭제 완료", popupId);
            }
        });

        // 2. 입찰 내역 하드 딜리트
        bidRepository.deleteAllByAuctionId(auctionId);
        log.info(">>>> [TestReset] auctionId={} 입찰 내역 삭제 완료", auctionId);

        // 3. AuctionOption.remainingStock 원복
        int resetCount = auctionOptionRepository.resetRemainingStockByAuctionId(auctionId);
        log.info(">>>> [TestReset] auctionId={} AuctionOption {} 개 재고 원복 완료", auctionId, resetCount);

        // 4. Auction 상태 원복
        auction.resetForTest();
        log.info(">>>> [TestReset] auctionId={} 상태 원복 완료 (OPEN, soldAt=null)", auctionId);

        log.info(">>>> [TestReset] auctionId={} DB 초기화 완료", auctionId);
    }

    /**
     * Redis 및 드로우 초기화 (steps 5–7), reset() 커밋 후 호출
     * 5. Redis 경매 재고 키 초기화
     * 6. Redis 경매 대기열 키 초기화
     * 7. 드로우 데이터 + 대기열 초기화 — draw-service Feign
     */
    public void resetRedisAndDraw(Long popupId) {
        Auction auction = auctionRepository.findByPopupId(popupId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));
        long auctionId = auction.getId();

        // Redis 경매 재고 키 초기화
        List<AuctionOption> options = auctionOptionRepository
            .findByAuction_IdOrderByEntryDateAscEntryTimeAsc(auctionId);

        for (AuctionOption option : options) {
            // DB에서 원복된 remainingStock 기준으로 Redis 재고 초기화
            bidRedisRepository.setAvailableStock(option.getId(), auction.getStockPerOption());
            redisTemplate.delete("auction:option:" + option.getId() + ":pending-restock");
        }
        redisTemplate.delete("auction:" + auctionId + ":sold-out-price");
        redisTemplate.delete("auction:" + auctionId + ":restock-anchor-at");
        log.info(">>>> [TestReset] auctionId={} Redis 경매 재고 초기화 완료", auctionId);

        // Redis 경매 대기열 키 초기화
        auctionQueueResetService.reset("auction", auctionId);

        // 드로우 초기화 (Feign - draw-service) — 실패 시 경고 로그 후 예외 전파
        try {
            drawServiceClient.resetDraw(popupId);
            log.info(">>>> [TestReset] popupId={} 드로우 초기화 완료 (draw-service)", popupId);
        } catch (Exception e) {
            log.warn(">>>> [TestReset] popupId={} 드로우 초기화 실패 (draw-service): {}", popupId, e.getMessage());
            throw e;
        }

        log.info(">>>> [TestReset] popupId={} 통합 초기화 완료", popupId);
    }
}
