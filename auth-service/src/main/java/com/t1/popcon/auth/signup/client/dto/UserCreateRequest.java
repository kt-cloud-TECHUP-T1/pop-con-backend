package com.t1.popcon.auth.signup.client.dto;

import com.t1.popcon.auth.oauth.dto.RegisterPayload;
import com.t1.popcon.auth.signup.dto.SignUpRequest;

public record UserCreateRequest(
    String provider,
    String providerUserId,
    String email,
    String nickname,
    String name,
    String profileImageUrl,
    String ciHash,
    Boolean isMarketingAgreed
) {
    public static UserCreateRequest of(RegisterPayload payload, SignUpRequest.Agreements agreements) {
        return new UserCreateRequest(
            payload.provider().name(),
            payload.providerUserId(),
            payload.email(),
            payload.nickname(),
            payload.name(),
            payload.profileImageUrl(),
            payload.ciHash(),
            agreements.isMarketingAgreed()
        );
    }
}
