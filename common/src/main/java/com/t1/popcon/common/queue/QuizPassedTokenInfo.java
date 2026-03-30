package com.t1.popcon.common.queue;

/**
 * 퀴즈 통과 토큰에서 추출한 사용자 식별 정보
 */
public record QuizPassedTokenInfo(
	String phaseType,  // draw / auction
	Long phaseId,      // drawId / auctionId
	Long userId
) {
}
