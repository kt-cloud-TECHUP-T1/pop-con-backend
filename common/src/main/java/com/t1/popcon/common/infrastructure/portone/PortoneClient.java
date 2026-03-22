package com.t1.popcon.common.infrastructure.portone;

import com.t1.popcon.common.infrastructure.dto.PortOneBillingKeyResponse;
import com.t1.popcon.common.infrastructure.dto.PortOneIdentityVerificationResponse;

/**
 * 포트원 API 클라이언트 인터페이스
 */
public interface PortOneClient {

    // 빌링키 정보 조회
    PortOneBillingKeyResponse fetchBillingKeyInfo(String customerUid);

    // 빌링키 결제 요청
    void executePayment(String billingKey, String merchantUid, int amount, String orderName);

    // 결제 취소
    void cancelPayment(String merchantUid, String reason);

    // 본인인증 결과 조회
    PortOneIdentityVerificationResponse fetchIdentityVerification(String identityVerificationId);
}