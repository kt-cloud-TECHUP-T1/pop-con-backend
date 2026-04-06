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

    private static final String MASKED_VALUE = "****";

    @Override
    public PortOneBillingKeyResponse fetchBillingKeyInfo(String billingKey) {
        log.info(">>>> [Local Mock PortOne] fetchBillingKeyInfo");
        log.debug(">>>> [Local Mock PortOne] fetchBillingKeyInfo billingKey={}", mask(billingKey));
        return new PortOneBillingKeyResponse(billingKey, "READY", null, null);
    }

    @Override
    public PortOnePaymentResponse executePayment(String billingKey, String merchantUid, int amount, String orderName) {
        log.info(">>>> [Local Mock PortOne] executePayment merchantUid={}, amount={}, orderName={}",
            merchantUid, amount, orderName);
        log.debug(">>>> [Local Mock PortOne] executePayment billingKey={}", mask(billingKey));

        return new PortOnePaymentResponse(
            new PortOnePaymentResponse.Payment(
                "mock-pg-tx-" + merchantUid,
                OffsetDateTime.now().toString()
            )
        );
    }

    @Override
    public PortOneCancelResponse cancelPayment(String paymentId, int amount) {
        log.info(">>>> [Local Mock PortOne] cancelPayment amount={}", amount);
        log.debug(">>>> [Local Mock PortOne] cancelPayment paymentId={}", mask(paymentId));

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
        log.info(">>>> [Local Mock PortOne] fetchIdentityVerification");
        log.debug(">>>> [Local Mock PortOne] fetchIdentityVerification identityVerificationId={}",
            mask(identityVerificationId));
        return new PortOneIdentityVerificationResponse(
            identityVerificationId,
            "VERIFIED",
            null
        );
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return MASKED_VALUE;
        }
        if (value.length() <= 4) {
            return MASKED_VALUE;
        }
        return MASKED_VALUE + value.substring(value.length() - 4);
    }
}
