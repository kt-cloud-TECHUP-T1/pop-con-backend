package com.t1.popcon.user.billing.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.billing.dto.BillingKeyInternalResponse;
import com.t1.popcon.user.billing.service.BillingKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/billing/keys")
@RequiredArgsConstructor
public class InternalBillingKeyController {

    private final BillingKeyService billingKeyService;

    @GetMapping("/default")
    public ApiResponse<BillingKeyInternalResponse> getDefaultBillingKey(
        @RequestParam("userId") Long userId
    ) {
        BillingKeyInternalResponse billingKey = billingKeyService.getDefaultBillingKeySnapshot(userId);
        return ApiResponse.ok(billingKey);
    }
}
