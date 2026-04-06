package com.t1.popcon.auction.bid.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BidStatus {
	PENDING("결제 대기"),			// Redis 선점 성공 후 결제 전
	SUCCESS("낙찰 성공"),			// 결제 완료 및 DB 반영 완료
	FAILED("낙찰 실패"),			// 결제 실패 또는 시스템 오류
	CANCELLED("취소됨");			// 낙찰 후 취소 처리

	private final String description;
}