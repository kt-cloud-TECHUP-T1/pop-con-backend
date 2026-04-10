package com.t1.popcon.ticket.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.ticket.domain.Ticket;
import com.t1.popcon.ticket.dto.request.TicketIssueRequest;
import com.t1.popcon.ticket.dto.response.TicketDetailResponse;
import com.t1.popcon.ticket.dto.response.TicketIssueResponse;
import com.t1.popcon.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketIssueService {

    private final TicketRepository ticketRepository;
    private final TicketNumberGenerator ticketNumberGenerator;

    @Transactional
    public TicketIssueResponse issue(TicketIssueRequest request) {
        Ticket existingTicket = ticketRepository.findActiveBySourceTypeAndSourceId(
                request.sourceType(),
                request.sourceId()
            )
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

        try {
            Ticket savedTicket = ticketRepository.saveAndFlush(ticket);
            savedTicket.assignTicketNumber(ticketNumberGenerator.generate(savedTicket.getId()));
            return TicketIssueResponse.from(savedTicket);
        } catch (DataIntegrityViolationException e) {
            Ticket duplicatedTicket = ticketRepository.findActiveBySourceTypeAndSourceId(
                    request.sourceType(),
                    request.sourceId()
                )
                .orElseThrow(() -> e);
            return TicketIssueResponse.from(duplicatedTicket);
        }
    }

    @Transactional(readOnly = true)
    public Slice<TicketDetailResponse> getTicketsByUserId(Long userId, Pageable pageable) {
        return ticketRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(TicketDetailResponse::from);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketByReservationNo(String reservationNo, Long userId) {
        if (reservationNo == null || reservationNo.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Ticket ticket = ticketRepository.findByReservationNoAndUserId(reservationNo, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));
        return TicketDetailResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketById(Long ticketId, Long userId) {
        if (ticketId == null || ticketId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));
        return TicketDetailResponse.from(ticket);
    }
}
