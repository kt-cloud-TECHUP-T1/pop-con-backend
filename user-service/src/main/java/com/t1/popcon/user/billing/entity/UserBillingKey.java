package com.t1.popcon.user.billing.entity;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_billing_key",
	indexes = {@Index(name = "idx_user_billing_active", columnList = "userId, isActive")})
public class UserBillingKey extends BaseSoftDeleteEntity {

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String customerUid; // 포트원 빌링키

	private String pgProvider;

	private String cardName;

	private String cardNumber;

	@Column(nullable = false)
	private boolean isActive = true;

	@Builder
	public UserBillingKey(Long userId, String customerUid, String pgProvider,
		String cardName, String cardNumber) {
		this.userId = userId;
		this.customerUid = customerUid;
		this.pgProvider = pgProvider;
		this.cardName = cardName;
		this.cardNumber = cardNumber;
		this.createdBy = userId;
	}

	public void deactivate() {
		this.isActive = false;
	}
}