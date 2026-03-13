package com.t1.popcon.user.billing.infrastructure;

import com.t1.popcon.user.billing.dto.PortoneBillingKeyResponse;

public interface PortoneClient {
	PortoneBillingKeyResponse fetchBillingKeyInfo(String customerUid);
}