package com.t1.popcon.auction.bid.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BidHistoryResponse {
	private Long id;
	private String thumbnailUrl;
	private String popupTitle;
	private Integer bidPrice;
	private LocalDateTime paidAt;
	private String displayStatus;
}
