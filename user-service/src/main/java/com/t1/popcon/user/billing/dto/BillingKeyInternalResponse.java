package com.t1.popcon.user.billing.dto;

import com.t1.popcon.user.billing.entity.UserBillingKey;

public record BillingKeyInternalResponse(
    String customerUid,
    String pgProvider,
    String cardName,
    String cardNumber
) {
    public static BillingKeyInternalResponse from(UserBillingKey entity) {
        return new BillingKeyInternalResponse(
            entity.getCustomerUid(),
            entity.getPgProvider(),
            entity.getCardName(),
            entity.getCardNumber()
        );
    }
}
