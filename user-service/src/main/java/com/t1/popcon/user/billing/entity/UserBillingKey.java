package com.t1.popcon.user.billing.entity;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import com.t1.popcon.user.domain.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_billing_key",
	indexes = {
		@Index(name = "idx_user_billing_active", columnList = "userId, isActive"),
		@Index(name = "idx_user_billing_default", columnList = "userId, isDefault, isActive")
	})
public class UserBillingKey extends BaseSoftDeleteEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_billing_key_user"))
	private User user;

	@Column(nullable = false)
	private String customerUid; // 포트원 빌링키

	private String pgProvider;

	private String cardName;

	private String cardNumber;

	@Column(nullable = false)
	private boolean isActive = true;

	@Column(nullable = false)
	private boolean isDefault = false;

	@Builder
	public UserBillingKey(User user, String customerUid, String pgProvider,
		String cardName, String cardNumber, boolean isDefault) {
		this.user = user;
		this.customerUid = customerUid;
		this.pgProvider = pgProvider;
		this.cardName = cardName;
		this.cardNumber = cardNumber;
		this.isDefault = isDefault;
		this.createdBy = user.getId();
	}

	public void deactivate() {
		this.isActive = false;
	}

	public void updateDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
}