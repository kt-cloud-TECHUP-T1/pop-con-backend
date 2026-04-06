package com.t1.popcon.auction.bid.dto;

import com.t1.popcon.auction.bid.domain.BidStatus;

public record BidResponse(
	Long bidId,
	BidStatus status,
	String message,
	String reservationNo
) {}