package com.t1.popcon.auction.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "auction")
@SQLRestriction("deleted = false")
public class Auction extends BaseSoftDeleteEntity {

    @Column(name = "popup_id", nullable = false, unique = true)
    private Long popupId;

    @Column(name = "start_price", nullable = false)
    private Integer startPrice;

    @Column(name = "minimum_price", nullable = false)
    private Integer minimumPrice;

    @Column(name = "price_drop_unit", nullable = false)
    private Integer priceDropUnit;

    @Column(name = "price_drop_interval_seconds", nullable = false)
    private Integer priceDropIntervalSeconds;

    @Column(name = "stock_per_option", nullable = false)
    private Integer stockPerOption;

    @Column(name = "open_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "close_at", nullable = false)
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuctionStatus status;


    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Builder
    public Auction(
            Long popupId,
            Integer startPrice,
            Integer minimumPrice,
            Integer priceDropUnit,
            Integer priceDropIntervalSeconds,
            Integer stockPerOption,
            LocalDateTime openedAt,
            LocalDateTime closedAt,
            AuctionStatus status,
            LocalDateTime soldAt
    ) {
        validatePricePolicy(
                popupId,
                startPrice,
                minimumPrice,
                priceDropUnit,
                priceDropIntervalSeconds,
                stockPerOption,
                openedAt,
                closedAt
        );

        this.popupId = popupId;
        this.startPrice = startPrice;
        this.minimumPrice = minimumPrice;
        this.priceDropUnit = priceDropUnit;
        this.priceDropIntervalSeconds = priceDropIntervalSeconds;
        this.stockPerOption = stockPerOption;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.status = status;
        this.soldAt = soldAt;
    }

    public boolean isSold() {
        return this.status == AuctionStatus.SOLD_OUT;
    }

    public void markOpen() {
        this.status = AuctionStatus.OPEN;
    }

    public void markClosed() {
        this.status = AuctionStatus.CLOSED;
    }

    public void updateStatus(AuctionStatus status) {
        this.status = status;
    }

    public void markSoldOut(Long winnerMemberId, LocalDateTime soldAt) {
        this.status = AuctionStatus.SOLD_OUT;
        this.soldAt = soldAt;
    }

    private void validatePricePolicy(
            Long popupId,
            Integer startPrice,
            Integer minimumPrice,
            Integer priceDropUnit,
            Integer priceDropIntervalSeconds,
            Integer stockPerOption,
            LocalDateTime openedAt,
            LocalDateTime closedAt
    ) {
        if (popupId == null || popupId <= 0) {
            throw new IllegalArgumentException("팝업이 있어야 합니다.");
        }
        if (startPrice == null || startPrice <= 0) {
            throw new IllegalArgumentException("시작가는 0보다 커야 합니다..");
        }
        if (minimumPrice == null || minimumPrice <= 0) {
            throw new IllegalArgumentException("최저가는 0보다 커야 합니다.");
        }
        if (startPrice < minimumPrice) {
            throw new IllegalArgumentException("시작가는 최저가보다 크거나 같아야 합니다.");
        }
        if (priceDropUnit == null || priceDropUnit <= 0) {
            throw new IllegalArgumentException("가격 감소 단위는 0보다 커야 합니다.");
        }
        if (priceDropIntervalSeconds == null || priceDropIntervalSeconds <= 0) {
            throw new IllegalArgumentException("가격 감소 주기는 0보다 커야 합니다.");
        }
        if (stockPerOption == null || stockPerOption <= 0) {
            throw new IllegalArgumentException("회차당 구매 가능 수량은 0보다 커야 합니다.");
        }
        if (openedAt == null || closedAt == null) {
            throw new IllegalArgumentException("경매 시작/종료 시각은 필수입니다.");
        }
        if (!openedAt.isBefore(closedAt)) {
            throw new IllegalArgumentException("경매 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }
}
