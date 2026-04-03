package com.t1.popcon.ticket.repository;

import com.t1.popcon.ticket.domain.Ticket;
import com.t1.popcon.ticket.domain.TicketSourceType;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsBySourceTypeAndSourceId(TicketSourceType sourceType, Long sourceId);

    Optional<Ticket> findBySourceTypeAndSourceId(TicketSourceType sourceType, Long sourceId);

    Optional<Ticket> findByReservationNo(String reservationNo);

    Slice<Ticket> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
        select t
        from Ticket t
        where t.sourceType = :sourceType
          and t.sourceId = :sourceId
    """)
    Optional<Ticket> findActiveBySourceTypeAndSourceId(
        @Param("sourceType") TicketSourceType sourceType,
        @Param("sourceId") Long sourceId
    );
}
