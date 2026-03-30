package com.t1.popcon.user.dto.history;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DrawHistoryResponse {
	private Long id;              // 응모 내역 ID
	private Long drawId;          // 드로우 ID
	private String thumbnailUrl;
	private String title;
	private Long price;
	private LocalDateTime paidAt;
	private String displayStatus;
	private String status;
}
