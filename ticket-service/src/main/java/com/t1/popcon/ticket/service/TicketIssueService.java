package com.t1.popcon.ticket.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.ticket.domain.Ticket;
import com.t1.popcon.ticket.dto.request.TicketIssueRequest;
import com.t1.popcon.ticket.dto.response.TicketDetailResponse;
import com.t1.popcon.ticket.dto.response.TicketIssueResponse;
import com.t1.popcon.ticket.repository.TicketRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketIssueService {

    private final TicketRepository ticketRepository;
    private final TicketNumberGenerator ticketNumberGenerator;

    @Transactional
    public TicketIssueResponse issue(TicketIssueRequest request) {
        Ticket existingTicket = ticketRepository.findBySourceTypeAndSourceId(request.sourceType(), request.sourceId())
            .orElse(null);
        if (existingTicket != null) {
            return TicketIssueResponse.from(existingTicket);
        }

        Ticket ticket = Ticket.builder()
            .userId(request.userId())
            .popupId(request.popupId())
            .sourceType(request.sourceType())
            .sourceId(request.sourceId())
            .reservationNo(request.reservationNo())
            .entryDate(request.entryDate())
            .entryTime(request.entryTime())
            .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        savedTicket.assignTicketNumber(ticketNumberGenerator.generate(savedTicket.getId()));
        return TicketIssueResponse.from(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketDetailResponse> getTicketsByUserId(Long userId) {
        return ticketRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(TicketDetailResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketByReservationNo(String reservationNo) {
        Ticket ticket = ticketRepository.findByReservationNo(reservationNo)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        return TicketDetailResponse.from(ticket);
    }
}
