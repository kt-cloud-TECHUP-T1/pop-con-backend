package com.t1.popcon.draw.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
    name = "draw_entries",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_draw_option",
            columnNames = {"user_id", "draw_option_id", "deleted"}
        ),
        @UniqueConstraint(
            name = "uk_user_draw",
            columnNames = {"user_id", "draw_id", "deleted"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class DrawEntry extends BaseSoftDeleteEntity {

    public static final String UNIQUE_CONSTRAINT_NAME = "uk_user_draw_option";
    public static final String DRAW_UNIQUE_CONSTRAINT_NAME = "uk_user_draw";

    @Column(nullable = false)
    private Long userId;

    @Column(name = "draw_id", nullable = false)
    private Long drawId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draw_option_id", nullable = false)
    private DrawOption drawOption;

    @Column(name = "encrypted_name", nullable = false)
    private String encryptedName;

    @Column(name = "encrypted_phone_number", nullable = false)
    private String encryptedPhoneNumber;

    @Column(nullable = false)
    private boolean isPrivacyAgreed;

    @Column(nullable = false)
    private boolean isTermsAgreed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrawEntryStatus status;

    private LocalDateTime paidAt;

    @Column(name = "result_checked_at")
    private LocalDateTime resultCheckedAt;

    @Column(name = "ticket_issued_at")
    private LocalDateTime ticketIssuedAt;

    @Builder
    public DrawEntry(
        Long userId,
        DrawOption drawOption,
        String encryptedName,
        String encryptedPhoneNumber,
        boolean isPrivacyAgreed,
        boolean isTermsAgreed
    ) {
        validate(userId, drawOption, encryptedName, encryptedPhoneNumber);

        this.userId = userId;
        this.drawId = drawOption.getDraw().getId();
        this.drawOption = drawOption;
        this.encryptedName = encryptedName;
        this.encryptedPhoneNumber = encryptedPhoneNumber;
        this.isPrivacyAgreed = isPrivacyAgreed;
        this.isTermsAgreed = isTermsAgreed;
        this.status = DrawEntryStatus.APPLIED;
    }

    public void markAsWinner() {
        this.status = DrawEntryStatus.WINNER;
    }

    public void markAsFailed() {
        this.status = DrawEntryStatus.FAILED;
    }

    public void completePayment(LocalDateTime paidAt) {
        if (this.paidAt == null) {
            this.paidAt = paidAt;
        }
    }

    public void markResultChecked(LocalDateTime checkedAt) {
        if (this.resultCheckedAt == null) {
            this.resultCheckedAt = checkedAt;
        }
    }

    public void markTicketIssued(LocalDateTime issuedAt) {
        if (this.ticketIssuedAt == null) {
            this.ticketIssuedAt = issuedAt;
        }
    }

    private void validate(
        Long userId,
        DrawOption drawOption,
        String encryptedName,
        String encryptedPhoneNumber
    ) {
        if (userId == null || userId <= 0 || drawOption == null || isBlank(encryptedName) || isBlank(encryptedPhoneNumber)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
