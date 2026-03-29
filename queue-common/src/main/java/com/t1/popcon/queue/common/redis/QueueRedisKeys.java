package com.t1.popcon.queue.common.redis;

import com.t1.popcon.queue.common.domain.PhaseType;

/**
 * 대기열 Redis 키 상수
 * - 모든 키는 이 클래스에서 중앙 관리
 */
public final class QueueRedisKeys {

    private QueueRedisKeys() {}

    // ── 키 패턴 (%s = phaseType, %d = phaseId) ───────────────────

    private static final String SEQ        = "queue:seq:%s:%d";             // 순번 채번 (STRING)
    private static final String WAITING    = "queue:waiting:%s:%d";         // 대기 명단 (ZSET)
    private static final String HEARTBEAT  = "queue:heartbeat:%s:%d";       // WAITING heartbeat (ZSET)
    private static final String ACTIVE     = "queue:active:%s:%d";          // 활성 유저 (ZSET)
    private static final String USER       = "queue:user:%s:%d:%d";         // 유저 상태 (%s=phaseType, %d=phaseId, %d=userId) (HASH)
    private static final String TOKEN      = "queue:token:%s";              // queueToken 역조회 — %s = SHA-256(token) (HASH)
    private static final String BLOCK      = "queue:block:%s:%d:%d";        // 차단 (%s=phaseType, %d=phaseId, %d=userId) (STRING)
    private static final String QUIZ_PASSED = "queue:quiz-passed-token:%s"; // 퀴즈 통과 토큰 — %s = SHA-256(token) (HASH)

    // ── HASH 필드명 ───────────────────────────────────────────────

    public static final String FIELD_STATUS                = "status";
    public static final String FIELD_QUEUE_TOKEN           = "queueToken";
    public static final String FIELD_QUIZ_PASSED_TOKEN     = "quizPassedToken"; // 퀴즈 통과 후 저장, cleanup 시 역참조용
    public static final String FIELD_PHASE_TYPE            = "phaseType";
    public static final String FIELD_PHASE_ID              = "phaseId";
    public static final String FIELD_USER_ID               = "userId";

    // ── 키 생성 메서드 ────────────────────────────────────────────
    // phaseType은 PhaseType.from()으로 정규화된 소문자 값을 키에 사용

    /** queue:seq:{type}:{id} */
    public static String seq(String phaseType, long phaseId) {
        return SEQ.formatted(resolvePhaseType(phaseType), phaseId);
    }

    /** queue:waiting:{type}:{id} */
    public static String waiting(String phaseType, long phaseId) {
        return WAITING.formatted(resolvePhaseType(phaseType), phaseId);
    }

    /** queue:heartbeat:{type}:{id} */
    public static String heartbeat(String phaseType, long phaseId) {
        return HEARTBEAT.formatted(resolvePhaseType(phaseType), phaseId);
    }

    /** queue:active:{type}:{id} */
    public static String active(String phaseType, long phaseId) {
        return ACTIVE.formatted(resolvePhaseType(phaseType), phaseId);
    }

    /** queue:user:{type}:{id}:{userId} */
    public static String user(String phaseType, long phaseId, long userId) {
        return USER.formatted(resolvePhaseType(phaseType), phaseId, userId);
    }

    /**
     * queue:token:{hashedToken}
     * - hashedToken: 호출부(QueueActiveRepository)에서 EncryptionService.generateHash()로 생성한 값
     * - raw token이 Redis 키 이름에 노출되지 않도록 해시를 호출부에서 주입
     */
    public static String token(String hashedToken) {
        return TOKEN.formatted(hashedToken);
    }

    /** queue:block:{type}:{id}:{userId} */
    public static String block(String phaseType, long phaseId, long userId) {
        return BLOCK.formatted(resolvePhaseType(phaseType), phaseId, userId);
    }

    /**
     * queue:quiz-passed-token:{hashedToken}
     * - hashedToken: 호출부(QueueActiveRepository)에서 EncryptionService.generateHash()로 생성한 값
     * - raw token이 Redis 키 이름에 노출되지 않도록 해시를 호출부에서 주입
     */
    public static String quizPassedToken(String hashedToken) {
        return QUIZ_PASSED.formatted(hashedToken);
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────

    /**
     * phaseType 검증 후 정규화된 소문자 값 반환
     * - PhaseType enum(draw/auction) 외 값은 CustomException(INVALID_INPUT)
     * - 검증된 getValue()를 Redis 키 세그먼트에 사용하여 케이스 오염 방지
     */
    private static String resolvePhaseType(String phaseType) {
        return PhaseType.from(phaseType).getValue();
    }
}
