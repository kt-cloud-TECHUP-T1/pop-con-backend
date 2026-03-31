package com.t1.popcon.ticket.repository;

import com.t1.popcon.ticket.domain.Ticket;
import com.t1.popcon.ticket.domain.TicketSourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsBySourceTypeAndSourceId(TicketSourceType sourceType, Long sourceId);

    Optional<Ticket> findBySourceTypeAndSourceId(TicketSourceType sourceType, Long sourceId);

    Optional<Ticket> findByReservationNo(String reservationNo);

    List<Ticket> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
