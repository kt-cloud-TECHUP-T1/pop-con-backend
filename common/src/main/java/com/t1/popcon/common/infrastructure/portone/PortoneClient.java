package com.t1.popcon.common.infrastructure.portone;

import com.t1.popcon.common.infrastructure.dto.PortoneBillingKeyResponse;

public interface PortoneClient {
	PortoneBillingKeyResponse fetchBillingKeyInfo(String customerUid);
	void executePayment(String billingKey, String merchantUid, int amount, String orderName);

	void cancelPayment(String merchantUid, String s);
}