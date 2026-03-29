package com.t1.popcon.draw.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
	name = "draw_entries",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_user_draw_option",
			columnNames = {"user_id", "draw_option_id", "deleted"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class DrawEntry extends BaseSoftDeleteEntity {

	@Column(nullable = false)
	private Long userId;

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

	@Builder
	public DrawEntry(
		Long userId,
		DrawOption drawOption,
		String encryptedName,
		String encryptedPhoneNumber,
		boolean isPrivacyAgreed,
		boolean isTermsAgreed
	) {
		this.userId = userId;
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

	public void completePayment() {
		this.paidAt = LocalDateTime.now();
	}
}