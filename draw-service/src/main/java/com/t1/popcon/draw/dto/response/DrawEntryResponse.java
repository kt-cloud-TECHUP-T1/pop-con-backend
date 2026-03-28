package com.t1.popcon.draw.dto.response;

import com.t1.popcon.draw.domain.DrawEntryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DrawEntryResponse {
	private Long id;                     // 응모 내역(Entry) ID
	private Long drawId;                 // 드로우(Draw) ID
	private String thumbnailUrl;
	private String title;
	private Long price;
	private LocalDateTime paidAt;        // 결제 완료 날짜 (미결제 시 null)
	private String displayStatus;        // 진행중, 응모 완료, 당첨, 미당첨 등 표시 문구
	private DrawEntryStatus status;      // 실제 DB 상태 (APPLIED, WINNER, FAILED)
}