package com.t1.popcon.user.dto;

import com.t1.popcon.user.domain.User;

import java.time.LocalDate;

/**
 * GET /users/me 프로필 조회 응답 DTO
 */
public record UserProfileResponse(
        Long id,
        String name,
        String nickname,
        String email,
        String phone,
        LocalDate birthDate,
        String gender,
        String profileImage,
        LocalDate joinDate
) {
    /** User 엔티티와 복호화된 민감정보로 응답 객체 생성 */
    public static UserProfileResponse of(User user, String name, String phone, String birthDate, String gender) {
        return new UserProfileResponse(
                user.getId(),
                name,
                user.getNickname(),
                user.getEmail(),
                phone,
                birthDate != null ? LocalDate.parse(birthDate) : null,
                gender,
                user.getProfileImageUrl(),
                user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null
        );
    }
}
