package com.t1.popcon.auction.bid.domain;

import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Table(name = "bids", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "auction_id", "status"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class Bid extends BaseSoftDeleteEntity {

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_option_id", nullable = false)
    private AuctionOption auctionOption;

    @Column(name = "bid_price", nullable = false)
    private Integer bidPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    @Column(nullable = false, unique = true)
    private String merchantUid;

    @Column(name = "pg_tx_id", length = 100)
    private String pgTxId;

    private LocalDateTime paidAt;

    @Column(name = "reservation_no", length = 20, unique = true)
    private String reservationNo;

    @Column(name = "popup_id")
    private Long popupId;

    @Column(name = "popup_title")
    private String popupTitle;

    @Column(name = "popup_address")
    private String popupAddress;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "entry_time")
    private LocalTime entryTime;

    @Column(name = "start_price")
    private Integer startPrice;

    @Builder
    public Bid(Long userId, AuctionOption auctionOption, Integer bidPrice, String merchantUid,
               String reservationNo, String popupTitle, String popupAddress, String thumbnailUrl,
               LocalDate entryDate, LocalTime entryTime, Integer startPrice) {
        if (userId == null || userId <= 0
                || auctionOption == null
                || bidPrice == null || bidPrice <= 0
                || merchantUid == null || merchantUid.isBlank()) {
            throw new IllegalArgumentException("Bid 생성에 필요한 필수 값이 누락되었습니다.");
        }
        this.auctionId = auctionOption.getAuction().getId();
        this.userId = userId;
        this.auctionOption = auctionOption;
        this.bidPrice = bidPrice;
        this.merchantUid = merchantUid;
        this.status = BidStatus.PENDING;

        this.reservationNo = reservationNo;
        this.popupId = auctionOption.getAuction().getPopupId(); // AuctionOption에서 직접 추출
        this.popupTitle = popupTitle;
        this.popupAddress = popupAddress;
        this.thumbnailUrl = thumbnailUrl;
        this.entryDate = entryDate;
        this.entryTime = entryTime;
        this.startPrice = startPrice;
    }

    public void completePayment(String pgTxId, LocalDateTime paidAt) {
        if (this.status != BidStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 결제를 완료할 수 있습니다.");
        }
        if (pgTxId == null || pgTxId.isBlank()) {
            throw new IllegalArgumentException("pgTxId는 null일 수 없습니다.");
        }
        if (paidAt == null) {
            throw new IllegalArgumentException("paidAt은 null일 수 없습니다.");
        }
        this.status = BidStatus.SUCCESS;
        this.pgTxId = pgTxId;
        this.paidAt = paidAt;
    }

    public void failBid() {
        if (this.status != BidStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 실패 처리할 수 있습니다.");
        }
        this.status = BidStatus.FAILED;
        this.paidAt = null;
    }
}
