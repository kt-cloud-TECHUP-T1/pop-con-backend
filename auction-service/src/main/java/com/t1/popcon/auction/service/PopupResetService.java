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
     * popupId 기준 팝업 전체 데이터 초기화
     * 처리 순서:
     * 1. 티켓 삭제 (ticket-service Feign) — popupId 기준 한 번에 처리
     * 2. 입찰 내역 하드 딜리트
     * 3. AuctionOption.remainingStock 원복
     * 4. Auction.status/soldAt 원복
     * 5. Redis 경매 재고 키 초기화
     * 6. Redis 경매 대기열 키 초기화
     * 7. 드로우 데이터 + 대기열 초기화 (draw-service Feign)
     */
    @Transactional
    public void reset(Long popupId) {
        log.info(">>>> [TestReset] popupId={} 통합 초기화 시작", popupId);

        Auction auction = auctionRepository.findByPopupId(popupId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));
        long auctionId = auction.getId();

        // 1. 티켓 삭제 (Feign - ticket-service)
        ticketServiceClient.deleteTicketsByPopupId(popupId);
        log.info(">>>> [TestReset] popupId={} 티켓 삭제 완료", popupId);

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
     * Redis 초기화 (트랜잭션 밖에서 호출)
     * - 경매 재고/메타 키 삭제
     * - 경매 대기열 키 삭제
     * - 드로우 초기화 (draw-service Feign)
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

        // 드로우 초기화 (Feign - draw-service)
        drawServiceClient.resetDraw(popupId);
        log.info(">>>> [TestReset] popupId={} 드로우 초기화 완료 (draw-service)", popupId);

        log.info(">>>> [TestReset] popupId={} 통합 초기화 완료", popupId);
    }
}
