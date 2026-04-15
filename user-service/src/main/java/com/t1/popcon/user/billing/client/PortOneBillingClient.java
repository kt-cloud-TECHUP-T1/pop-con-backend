package com.t1.popcon.user.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "portOneBillingClient", url = "https://api.portone.io")
public interface PortOneBillingClient {

    @PostMapping("/billing-keys")
    PortOneBillingResponse issueBillingKey(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PortOneBillingRequest request
    );

    record PortOneBillingRequest(
            String channelKey,
            Customer customer,
            Method method
    ) {
        public record Customer(String id) {}
        public record Method(Card card) {
            public record Card(Credential credential) {
                public record Credential(
                        String number,
                        String expiryYear,
                        String expiryMonth,
                        String birthOrBusinessRegistrationNumber,
                        String passwordTwoDigits
                ) {}
            }
        }
    }

    record PortOneBillingResponse(
            BillingKeyInfo billingKeyInfo
    ) {
        public record BillingKeyInfo(
                String billingKey,
                java.util.List<Channel> channels
        ) {
            public record Channel(String name, String pgProvider) {}
        }
    }
}
