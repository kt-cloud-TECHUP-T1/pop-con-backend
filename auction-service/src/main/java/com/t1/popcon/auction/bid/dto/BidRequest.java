package com.t1.popcon.auction.bid.dto;

import jakarta.validation.constraints.NotNull;

public record BidRequest(
	@NotNull(message = "경매 회차 정보는 필수입니다.")
	Long auctionOptionId,

	@NotNull(message = "사용자가 확인한 가격은 필수입니다.")
	Integer bidPrice // 클라이언트가 버튼을 누른 시점의 가격 (검증용)
) {}