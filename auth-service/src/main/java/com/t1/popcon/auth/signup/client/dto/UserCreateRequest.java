package com.t1.popcon.auth.signup.client.dto;

import com.t1.popcon.auth.common.RegisterPayload;
import com.t1.popcon.auth.signup.dto.SignUpRequest;

public record UserCreateRequest(
    String provider,
    String providerUserId,
    String email,
    String profileImageUrl,
    
    String ciHash,
    String encryptedName,
    String encryptedPhoneNumber,
    String encryptedBirthDate,
    String encryptedGender,
    String encryptedNationality,
    
    Boolean isMarketingAgreed
) {
    public static UserCreateRequest of(RegisterPayload payload, SignUpRequest.Agreements agreements) {
        return new UserCreateRequest(
            payload.provider().name(),
            payload.providerUserId(),
            payload.email(),
            payload.profileImageUrl(),
            payload.ciHash(),
            payload.encryptedName(),
            payload.encryptedPhoneNumber(),
            payload.encryptedBirthDate(),
            payload.encryptedGender(),
            payload.encryptedNationality(),
            agreements.isMarketingAgreed()
        );
    }
}
