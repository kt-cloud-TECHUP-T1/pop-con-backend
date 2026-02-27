package com.t1.popcon.auth.oauth.dto;

import com.t1.popcon.auth.oauth.service.OAuthProvider;

/**
 * 2단계(토큰/유저정보)까지 붙였는지 확인하기 위한 임시 응답
 * - DB 연동/로그인 토큰 발급 전에, "카카오/네이버에서 유저정보 뽑히는지" 먼저 검증한다.
 *
 * 확인이 끝나면 SocialLoginResponse(기존/신규 분기)로 교체하면 됨.
 */
public record OAuthCallbackDebugResponse(
        OAuthProvider provider,
        OAuthUserInfo userInfo
) { }
