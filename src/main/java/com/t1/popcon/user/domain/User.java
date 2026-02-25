package com.t1.popcon.user.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_ci_hash", columnNames = "ci_hash"),
        @UniqueConstraint(name = "uk_users_kakao_user_id", columnNames = "kakao_user_id"),
        @UniqueConstraint(name = "uk_users_naver_user_id", columnNames = "naver_user_id")
    },
    indexes = {
        @Index(name = "idx_users_status", columnList = "status"),
        @Index(name = "idx_users_role", columnList = "role")
    }
)
public class User extends BaseSoftDeleteEntity {

    // 유저 식별
    @Column(name = "ci_hash", length = 64, nullable = false)
    private String ciHash;

    @Column(name = "ci_verified_at", nullable = false)
    private LocalDateTime ciVerifiedAt;

    // 개인 식별 정보
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    // 소셜 연결
    @Column(name = "kakao_user_id", length = 128)
    private String kakaoUserId;

    @Column(name = "kakao_connected_at")
    private LocalDateTime kakaoConnectedAt;

    @Column(name = "naver_user_id", length = 128)
    private String naverUserId;

    @Column(name = "naver_connected_at")
    private LocalDateTime naverConnectedAt;

    // 권한/상태
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    // 내부 공통 로직
    private static User.UserBuilder baseBuilder(
        String ciHash,
        LocalDateTime ciVerifiedAt,
        String name,
        String phone,
        LocalDate birthDate,
        Gender gender
    ) {
        return User.builder()
            .ciHash(ciHash)
            .ciVerifiedAt(ciVerifiedAt)
            .name(name)
            .phone(phone)
            .birthDate(birthDate)
            .gender(gender)
            .role(Role.USER)
            .status(UserStatus.ACTIVE);
    }

    /**
     * 카카오로 회원 생성
     */
    public static User createUserWithKakao(
        String ciHash,
        LocalDateTime ciVerifiedAt,
        String name,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String kakaoUserId
    ) {
        User user = baseBuilder(ciHash, ciVerifiedAt, name, phone, birthDate, gender).build();
        user.connectKakao(kakaoUserId, LocalDateTime.now());
        return user;
    }

    /**
     * 네이버로 회원 생성
     */
    public static User createUserWithNaver(
        String ciHash,
        LocalDateTime ciVerifiedAt,
        String name,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String naverUserId
    ) {
        User user = baseBuilder(ciHash, ciVerifiedAt, name, phone, birthDate, gender).build();
        user.connectNaver(naverUserId, LocalDateTime.now());
        return user;
    }

    public void connectKakao(String kakaoUserId, LocalDateTime connectedAt) {
        this.kakaoUserId = kakaoUserId;
        this.kakaoConnectedAt = connectedAt;
    }

    public void connectNaver(String naverUserId, LocalDateTime connectedAt) {
        this.naverUserId = naverUserId;
        this.naverConnectedAt = connectedAt;
    }

    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void softDelete(Long deleterId) {
        super.markDeleted(deleterId);
    }
}