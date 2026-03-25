package com.t1.popcon.user.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Entity
@Table(
    name = "user",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_ci_hash", columnNames = "ci_hash"),
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname"),
        @UniqueConstraint(name = "uk_users_kakao_user_id", columnNames = "kakao_user_id"),
        @UniqueConstraint(name = "uk_users_naver_user_id", columnNames = "naver_user_id")
    },
    indexes = {
        @Index(name = "idx_users_status", columnList = "status"),
        @Index(name = "idx_users_role", columnList = "role")
    }
)
@SQLDelete(sql = "UPDATE user SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
public class User extends BaseSoftDeleteEntity {

    @Column(name = "ci_hash", length = 64, nullable = false)
    private String ciHash;

    @Column(name = "encrypted_name", length = 255, nullable = false)
    private String encryptedName;

    @Column(name = "encrypted_phone_number", length = 255, nullable = false)
    private String encryptedPhoneNumber;

    @Column(name = "encrypted_birth_date", length = 255, nullable = false)
    private String encryptedBirthDate;

    @Column(name = "encrypted_gender", length = 255)
    private String encryptedGender;

    @Column(name = "encrypted_nationality", length = 255)
    private String encryptedNationality;

    @Column(name = "nickname", length = 50, unique = true)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "kakao_user_id", length = 128)
    private String kakaoUserId;

    @Column(name = "kakao_connected_at")
    private LocalDateTime kakaoConnectedAt;

    @Column(name = "naver_user_id", length = 128)
    private String naverUserId;

    @Column(name = "naver_connected_at")
    private LocalDateTime naverConnectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    private static UserBuilder baseBuilder(
            String ciHash,
            String encryptedName,
            String encryptedPhoneNumber,
            String encryptedBirthDate,
            String encryptedGender,
            String encryptedNationality,
            String email
    ) {
        return User.builder()
                .ciHash(ciHash)
                .encryptedName(encryptedName)
                .encryptedPhoneNumber(encryptedPhoneNumber)
                .encryptedBirthDate(encryptedBirthDate)
                .encryptedGender(encryptedGender)
                .encryptedNationality(encryptedNationality)
                .email(email)
                .role(Role.USER)
                .status(UserStatus.ACTIVE);
    }

    public static User createUserWithKakao(
            String ciHash,
            String encryptedName,
            String encryptedPhoneNumber,
            String encryptedBirthDate,
            String encryptedGender,
            String encryptedNationality,
            String email,
            String kakaoUserId
    ) {
        User user = baseBuilder(
                ciHash,
                encryptedName,
                encryptedPhoneNumber,
                encryptedBirthDate,
                encryptedGender,
                encryptedNationality,
                email
        ).build();

        user.connectKakao(kakaoUserId, LocalDateTime.now());
        return user;
    }

    public static User createUserWithNaver(
            String ciHash,
            String encryptedName,
            String encryptedPhoneNumber,
            String encryptedBirthDate,
            String encryptedGender,
            String encryptedNationality,
            String email,
            String naverUserId
    ) {
        User user = baseBuilder(
                ciHash,
                encryptedName,
                encryptedPhoneNumber,
                encryptedBirthDate,
                encryptedGender,
                encryptedNationality,
                email
        ).build();

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
