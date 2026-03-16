package com.t1.popcon.draw.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(
    name = "draw_options",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_draw_options_schedule",
            columnNames = {"draw_id", "entry_date", "entry_time"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class DrawOption extends BaseSoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draw_id", nullable = false)
    private Draw draw;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private LocalTime entryTime;

    @Builder
    public DrawOption(
        Draw draw,
        LocalDate entryDate,
        LocalTime entryTime
    ) {
        validate(draw, entryDate, entryTime);

        this.draw = draw;
        this.entryDate = entryDate;
        this.entryTime = entryTime;
    }

    private void validate(
        Draw draw,
        LocalDate entryDate,
        LocalTime entryTime
    ) {
        if (draw == null || entryDate == null || entryTime == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}