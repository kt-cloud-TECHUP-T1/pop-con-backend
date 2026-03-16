package com.t1.popcon.draw.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "draws")
@SQLRestriction("deleted = false")
public class Draw extends BaseSoftDeleteEntity {

    @Column(nullable = false, unique = true)
    private Long popupId;

    @Column(nullable = false)
    private LocalDateTime drawOpenAt;

    @Column(nullable = false)
    private LocalDateTime drawCloseAt;

    @Column(nullable = false)
    private Integer stockPerOption;

    @Builder
    public Draw(
            Long popupId,
            LocalDateTime drawOpenAt,
            LocalDateTime drawCloseAt,
            Integer stockPerOption
    ) {
        validateStockPerOption(stockPerOption);
        this.popupId = popupId;
        this.drawOpenAt = drawOpenAt;
        this.drawCloseAt = drawCloseAt;
        this.stockPerOption = stockPerOption;
    }

    public void updateSchedule(LocalDateTime drawOpenAt, LocalDateTime drawCloseAt) {
        this.drawOpenAt = drawOpenAt;
        this.drawCloseAt = drawCloseAt;
    }

    public void updateStockPerOption(Integer stockPerOption) {
        validateStockPerOption(stockPerOption);
        this.stockPerOption = stockPerOption;
    }

    private void validateStockPerOption(Integer stockPerOption) {
        if (stockPerOption == null ||  stockPerOption <= 0) {
            throw new IllegalArgumentException("회차당 당첨 수량은 0보다 커야 합니다.");
        }
    }
}