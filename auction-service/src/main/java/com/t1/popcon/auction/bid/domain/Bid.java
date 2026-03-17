package com.t1.popcon.auction.bid.domain;

import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "bids")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class Bid extends BaseSoftDeleteEntity {

	@Column(nullable = false)
	private Long userId; // 낙찰자 ID

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "auction_option_id", nullable = false)
	private AuctionOption auctionOption; // 낙찰된 경매 회차

	@Column(nullable = false)
	private Integer bidPrice; // 낙찰 확정 가격

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BidStatus status; // 낙찰 진행 상태

	@Column(nullable = false, unique = true)
	private String merchantUid; // 결제 중복 방지용 고유 번호

	private LocalDateTime paidAt; // 결제 완료 시점

	@Builder
	public Bid(Long userId, AuctionOption auctionOption, Integer bidPrice, String merchantUid) {
		if (userId == null || auctionOption == null || bidPrice == null || merchantUid == null) {
			throw new IllegalArgumentException("Bid 생성에 필요한 필수 값이 누락되었습니다.");
		}
		this.userId = userId;
		this.auctionOption = auctionOption;
		this.bidPrice = bidPrice;
		this.merchantUid = merchantUid;
		this.status = BidStatus.PENDING; // 생성 시 기본값
	}

	// 비즈니스 로직: 결제 성공 시 상태 업데이트
	public void completePayment(LocalDateTime paidAt) {
		if (this.status != BidStatus.PENDING) {
			throw new IllegalStateException("PENDING 상태에서만 결제를 완료할 수 있습니다.");
		}
		if (paidAt == null) {
			throw new IllegalArgumentException("paidAt은 null일 수 없습니다.");
		}
		this.status = BidStatus.SUCCESS;
		this.paidAt = paidAt;
	}

	// 비즈니스 로직: 낙찰 실패 처리
	public void failBid() {
		if (this.status != BidStatus.PENDING) {
			throw new IllegalStateException("PENDING 상태에서만 실패 처리할 수 있습니다.");
		}
		this.status = BidStatus.FAILED;
		this.paidAt = null;
	}
}