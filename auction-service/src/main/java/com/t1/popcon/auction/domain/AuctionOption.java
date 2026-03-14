package com.t1.popcon.auction.domain;

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
    name = "auction_options",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_auction_options_schedule",
            columnNames = {"auction_id", "entry_date", "entry_time"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class AuctionOption extends BaseSoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private LocalTime entryTime;

    // 총 재고는 auction.stockPerOption 기준, 현재 남은 재고만 관리
    @Column(nullable = false)
    private Integer remainingStock;

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // 낙관적 락

    @Builder
    public AuctionOption(
        Auction auction,
        LocalDate entryDate,
        LocalTime entryTime,
        Integer remainingStock
    ) {
        validate(auction, entryDate, entryTime, remainingStock);

        this.auction = auction;
        this.entryDate = entryDate;
        this.entryTime = entryTime;
        this.remainingStock = remainingStock;
    }

    public boolean isSelectable() {
        return remainingStock > 0;
    }

    public void decreaseStock(int quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CustomException(ErrorCode.AUCTION_OPTION_STOCK_INVALID);
        }
        if (remainingStock < quantity) {
            throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
        }
        this.remainingStock -= quantity;
    }

    private void validate(
        Auction auction,
        LocalDate entryDate,
        LocalTime entryTime,
        Integer remainingStock
    ) {
        if (auction == null || entryDate == null || entryTime == null || remainingStock == null) {
            throw new CustomException(ErrorCode.AUCTION_OPTION_STOCK_INVALID);
        }
        if (remainingStock < 0 || remainingStock > auction.getStockPerOption()) {
            throw new CustomException(ErrorCode.AUCTION_OPTION_STOCK_INVALID);
        }
    }
}