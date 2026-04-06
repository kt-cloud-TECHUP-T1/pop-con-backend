package com.t1.popcon.auth.oauth.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import java.util.Locale;

/**
 * 지원하는 OAuth Provider 정의
 */
public enum OAuthProvider {

    KAKAO,
    NAVER;

    /**
     * 문자열을 OAuthProvider로 변환
     * ex) "kakao" -> KAKAO
     */
    public static OAuthProvider from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }

        // 대소문자 무관 처리
        try {
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            return OAuthProvider.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // valueOf 실패(지원하지 않는 provider)
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }
    }

    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }
}