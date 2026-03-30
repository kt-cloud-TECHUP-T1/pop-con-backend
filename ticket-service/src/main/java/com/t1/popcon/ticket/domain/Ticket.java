package com.t1.popcon.ticket.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
    name = "tickets",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tickets_source_type_source_id_deleted",
            columnNames = {"source_type", "source_id", "deleted"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class Ticket extends BaseSoftDeleteEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "popup_id", nullable = false)
    private Long popupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private TicketSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Builder
    public Ticket(
        Long userId,
        Long popupId,
        TicketSourceType sourceType,
        Long sourceId,
        LocalDate entryDate,
        LocalTime entryTime
    ) {
        validate(userId, popupId, sourceType, sourceId, entryDate, entryTime);
        this.userId = userId;
        this.popupId = popupId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.entryDate = entryDate;
        this.entryTime = entryTime;
        this.status = TicketStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
    }

    public void markUsed() {
        if (this.status != TicketStatus.ISSUED) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        this.status = TicketStatus.USED;
    }

    public void cancel() {
        if (this.status == TicketStatus.CANCELLED) {
            return;
        }
        this.status = TicketStatus.CANCELLED;
    }

    private void validate(
        Long userId,
        Long popupId,
        TicketSourceType sourceType,
        Long sourceId,
        LocalDate entryDate,
        LocalTime entryTime
    ) {
        if (userId == null || userId <= 0
            || popupId == null || popupId <= 0
            || sourceType == null
            || sourceId == null || sourceId <= 0
            || entryDate == null
            || entryTime == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
