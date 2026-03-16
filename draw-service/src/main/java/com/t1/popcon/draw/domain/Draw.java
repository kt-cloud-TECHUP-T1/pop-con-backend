package com.t1.popcon.draw.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "draws")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Draw extends BaseSoftDeleteEntity {

    @Column(nullable = false, unique = true)
    private Long popupId;

    @Column(nullable = false)
    private LocalDateTime drawOpenAt;

    @Column(nullable = false)
    private LocalDateTime drawCloseAt;

    @Column(nullable = false)
    private Integer maxDrawQuantityPerRound;

    @Builder
    public Draw(
            Long popupId,
            LocalDateTime drawOpenAt,
            LocalDateTime drawCloseAt,
            Integer maxDrawQuantityPerRound
    ) {
        this.popupId = popupId;
        this.drawOpenAt = drawOpenAt;
        this.drawCloseAt = drawCloseAt;
        this.maxDrawQuantityPerRound = maxDrawQuantityPerRound;
    }

    public void updateSchedule(LocalDateTime drawOpenAt, LocalDateTime drawCloseAt) {
        this.drawOpenAt = drawOpenAt;
        this.drawCloseAt = drawCloseAt;
    }

    public void updateMaxDrawQuantityPerRound(Integer maxDrawQuantityPerRound) {
        this.maxDrawQuantityPerRound = maxDrawQuantityPerRound;
    }
}