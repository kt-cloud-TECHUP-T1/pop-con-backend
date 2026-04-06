package com.t1.popcon.auth.oauth.service;

/**
 * OAuth state 저장소 인터페이스
 *
 * 목적:
 * - OAuth CSRF 공격 방지
 * - state 값 1회성 검증
 *
 * 구현체:
 * - Redis 기반 구현
 */
public interface OAuthStateStore {

    /**
     * state 저장
     */
    void save(String state, OAuthProvider provider, long ttlSeconds);

    /**
     * state 소비(1회성)
     * - 존재하면 삭제 후 true
     * - 없으면 false
     */
    boolean consume(String state, OAuthProvider provider);
}