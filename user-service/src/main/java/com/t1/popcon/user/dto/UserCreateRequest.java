package com.t1.popcon.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
    @NotBlank String provider,
    @NotBlank String providerUserId,
    String email,
    String profileImageUrl,
    
    @NotBlank String ciHash,
    @NotBlank String encryptedName,
    @NotBlank String encryptedPhoneNumber,
    @NotBlank String encryptedBirthDate,
    String encryptedGender,
    String encryptedNationality,
    String phoneHash,
    
    @NotNull Boolean isMarketingAgreed
) {}
