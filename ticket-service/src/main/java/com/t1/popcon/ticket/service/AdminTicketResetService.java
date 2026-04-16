// 테스트용 티켓 초기화 서비스
package com.t1.popcon.ticket.service;

import com.t1.popcon.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTicketResetService {

    private final TicketRepository ticketRepository;

    // popupId에 해당하는 티켓 전체 하드 딜리트
    @Transactional
    public void deleteByPopupId(Long popupId) {
        ticketRepository.deleteAllByPopupId(popupId);
        log.info(">>>> [TestReset] popupId={} 티켓 삭제 완료", popupId);
    }
}
