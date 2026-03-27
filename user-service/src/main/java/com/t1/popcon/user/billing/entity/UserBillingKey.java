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
	indexes = {@Index(name = "idx_user_billing_active", columnList = "user_Id, isActive")})
public class UserBillingKey extends BaseSoftDeleteEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_billing_key_user_id"))
	private User user;

	@Column(nullable = false)
	private String customerUid; // 포트원 빌링키

	private String pgProvider;

	private String cardName;

	private String cardNumber;

	@Column(nullable = false)
	private boolean isActive = true;

	@Builder
	public UserBillingKey(User user, String customerUid, String pgProvider,
		String cardName, String cardNumber) {
		this.user = user;
		this.customerUid = customerUid;
		this.pgProvider = pgProvider;
		this.cardName = cardName;
		this.cardNumber = cardNumber;
		if (user != null) {
			this.createdBy = user.getId();
		}
	}

	public void deactivate() {
		this.isActive = false;
	}
}