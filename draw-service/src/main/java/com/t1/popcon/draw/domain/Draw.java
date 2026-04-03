package com.t1.popcon.draw.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "draws")
@SQLRestriction("deleted = false")
public class Draw extends BaseSoftDeleteEntity {

    private static final LocalTime ANNOUNCEMENT_TIME = LocalTime.of(11, 0);

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
        validateSchedule(drawOpenAt, drawCloseAt);
        validateStockPerOption(stockPerOption);

        this.popupId = popupId;
        this.drawOpenAt = drawOpenAt;
        this.drawCloseAt = drawCloseAt;
        this.stockPerOption = stockPerOption;
    }

    public void updateSchedule(LocalDateTime drawOpenAt, LocalDateTime drawCloseAt) {
        validateSchedule(drawOpenAt, drawCloseAt);
        this.drawOpenAt = drawOpenAt;
        this.drawCloseAt = drawCloseAt;
    }

    public void updateStockPerOption(Integer stockPerOption) {
        validateStockPerOption(stockPerOption);
        this.stockPerOption = stockPerOption;
    }

    public LocalDateTime getAnnouncementAt() {
        return drawCloseAt.toLocalDate().plusDays(1).atTime(ANNOUNCEMENT_TIME);
    }

    private void validateSchedule(LocalDateTime drawOpenAt, LocalDateTime drawCloseAt) {
        if (drawOpenAt == null || drawCloseAt == null || !drawCloseAt.isAfter(drawOpenAt)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateStockPerOption(Integer stockPerOption) {
        if (stockPerOption == null || stockPerOption <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
