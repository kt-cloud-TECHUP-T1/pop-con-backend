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
                formatPhone(phone),
                (birthDate != null && !birthDate.isBlank()) ? LocalDate.parse(birthDate.trim()) : null,
                gender,
                user.getProfileImageUrl(),
                user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null
        );
    }

    /** 전화번호를 010-XXXX-XXXX 형식으로 변환 */
    private static String formatPhone(String phone) {
        if (phone == null || phone.isBlank() || phone.contains("-")) {
            return phone;
        }
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
        }
        return phone;
    }
}
