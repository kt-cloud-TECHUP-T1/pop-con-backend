package com.t1.popcon.user.dto;

/**
 * PATCH /users/me/profile 프로필 수정 응답 DTO
 */
public record UserProfileUpdateResponse(
        String nickname,
        String profileImage
) {
}
