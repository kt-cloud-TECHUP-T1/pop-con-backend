package com.t1.popcon.auth.oauth.service;

import java.util.Locale;

/**
 * 지원하는 OAuth Provider 정의
 *
 * - path variable로 들어오는 provider 문자열을 안전하게 enum으로 변환
 * - 오타 방지 및 허용 provider 제한 목적
 */
public enum OAuthProvider {

    KAKAO,
    NAVER;

    /**
     * 문자열을 OAuthProvider로 변환
     * ex) "kakao" -> KAKAO
     */
    public static OAuthProvider from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("provider is null");
        }

        // 대소문자 무관 처리
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return OAuthProvider.valueOf(normalized);
    }

    /**
     * 소문자 형태 반환 (redirect uri 구성 시 사용)
     */
    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }
}