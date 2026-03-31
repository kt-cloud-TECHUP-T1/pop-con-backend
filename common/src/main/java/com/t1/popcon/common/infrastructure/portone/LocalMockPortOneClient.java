package com.t1.popcon.common.infrastructure.portone;

import com.t1.popcon.common.infrastructure.dto.PortOneBillingKeyResponse;
import com.t1.popcon.common.infrastructure.dto.PortOneCancelResponse;
import com.t1.popcon.common.infrastructure.dto.PortOneIdentityVerificationResponse;
import com.t1.popcon.common.infrastructure.dto.PortOnePaymentResponse;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
public class LocalMockPortOneClient implements PortOneClient {

    @Override
    public PortOneBillingKeyResponse fetchBillingKeyInfo(String billingKey) {
        log.info(">>>> [Local Mock PortOne] fetchBillingKeyInfo billingKey={}", billingKey);
        return new PortOneBillingKeyResponse(billingKey, "READY", null, null);
    }

    @Override
    public PortOnePaymentResponse executePayment(String billingKey, String merchantUid, int amount, String orderName) {
        log.info(">>>> [Local Mock PortOne] executePayment billingKey={}, merchantUid={}, amount={}, orderName={}",
            billingKey, merchantUid, amount, orderName);

        return new PortOnePaymentResponse(
            new PortOnePaymentResponse.Payment(
                "mock-pg-tx-" + merchantUid,
                OffsetDateTime.now().toString()
            )
        );
    }

    @Override
    public PortOneCancelResponse cancelPayment(String paymentId, int amount) {
        log.info(">>>> [Local Mock PortOne] cancelPayment paymentId={}, amount={}", paymentId, amount);

        return new PortOneCancelResponse(
            new PortOneCancelResponse.Cancellation(
                "SUCCEEDED",
                "mock-cancel-" + paymentId,
                "mock-pg-cancel-" + paymentId,
                amount,
                "local mock cancel",
                OffsetDateTime.now().toString()
            )
        );
    }

    @Override
    public PortOneIdentityVerificationResponse fetchIdentityVerification(String identityVerificationId) {
        log.info(">>>> [Local Mock PortOne] fetchIdentityVerification identityVerificationId={}", identityVerificationId);
        return new PortOneIdentityVerificationResponse(
            identityVerificationId,
            "VERIFIED",
            null
        );
    }
}
