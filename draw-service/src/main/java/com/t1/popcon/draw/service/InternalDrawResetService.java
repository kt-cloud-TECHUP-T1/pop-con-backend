// 테스트용 드로우 데이터 초기화 서비스 (내부 호출 전용)
package com.t1.popcon.draw.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.draw.client.TicketServiceClient;
import com.t1.popcon.draw.domain.Draw;
import com.t1.popcon.draw.repository.DrawEntryRepository;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import com.t1.popcon.draw.repository.DrawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalDrawResetService {

    private final DrawRepository drawRepository;
    private final DrawEntryRepository drawEntryRepository;
    private final DrawOptionRepository drawOptionRepository;
    private final DrawQueueResetService drawQueueResetService;
    private final TicketServiceClient ticketServiceClient;

    /**
     * popupId 기준 드로우 DB 데이터 초기화 (트랜잭션)
     * 1. draw_entries 하드 딜리트
     * 2. draw_options.processed 원복
     * Redis 대기열 초기화는 resetQueueByPopupId() 에서 처리
     * 티켓은 auction-service의 통합 초기화 시 popupId 기준으로 한 번에 처리
     */
    @Transactional
    public void resetByPopupId(Long popupId) {
        log.info(">>>> [TestReset] popupId={} 드로우 초기화 시작", popupId);

        // popupId로 드로우 조회
        Draw draw = drawRepository.findByPopupId(popupId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_NOT_FOUND));

        long drawId = draw.getId();

        // 응모 내역 하드 딜리트
        drawEntryRepository.deleteAllByDrawId(drawId);
        log.info(">>>> [TestReset] drawId={} 응모 내역 삭제 완료", drawId);

        // DrawOption processed 원복
        int resetCount = drawOptionRepository.resetProcessedByDrawId(drawId);
        log.info(">>>> [TestReset] drawId={} DrawOption {} 개 processed 원복 완료", drawId, resetCount);

        log.info(">>>> [TestReset] popupId={} 드로우 초기화 완료", popupId);
    }

    // 대기열 Redis 초기화 (트랜잭션 밖에서 호출)
    public void resetQueueByPopupId(Long popupId) {
        Draw draw = drawRepository.findByPopupId(popupId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_NOT_FOUND));

        drawQueueResetService.reset("draw", draw.getId());
    }

    // DB 초기화 + Redis 대기열 초기화를 단일 진입점으로 묶어 컨트롤러 단순화
    public void resetAllByPopupId(Long popupId) {
        resetByPopupId(popupId);
        resetQueueByPopupId(popupId);
    }
}
