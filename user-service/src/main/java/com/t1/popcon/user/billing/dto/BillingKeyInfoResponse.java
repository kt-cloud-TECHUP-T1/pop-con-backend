package com.t1.popcon.user.billing.dto;

import com.t1.popcon.user.billing.entity.UserBillingKey;
import java.time.LocalDateTime;

public record BillingKeyInfoResponse(
	Long id,
	String cardName,
	String cardNumber,
	boolean isDefault,
	LocalDateTime registeredAt
) {
	public static BillingKeyInfoResponse from(UserBillingKey entity) {
		return new BillingKeyInfoResponse(
			entity.getId(),
			entity.getCardName(),
			entity.getCardNumber(),
			entity.isDefault(),
			entity.getCreatedAt()
		);
	}
}