package com.t1.popcon.auction.bid.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BidRequest(
	@NotNull(message = "경매 회차 정보는 필수입니다.")
	@Positive(message = "경매 회차 ID는 1 이상이어야 합니다.")
	Long auctionOptionId,

	@NotNull(message = "사용자가 확인한 가격은 필수입니다.")
	@Positive(message = "입찰 가격은 1원 이상이어야 합니다.")
	Integer bidPrice // 클라이언트가 버튼을 누른 시점의 가격 (검증용)
) {}