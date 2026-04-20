package com.t1.popcon.auction.bid.client.dto;

public record BillingKeyInternalResponse(
    String customerUid,
    String pgProvider,
    String cardName,
    String cardNumber
) {}
