package com.t1.popcon.auth.identity.service;

import com.t1.popcon.auth.identity.dto.IdentityRequest;
import com.t1.popcon.auth.identity.dto.IdentityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityService {

    public IdentityResponse.NewUserComplete complete(IdentityRequest.Complete request, String deviceId) {
        log.info("본인인증 완료(임시) - identityVerificationId: {}, deviceId: {}",
            request.identityVerificationId(), deviceId);

        // TODO: registerToken Redis 저장(TTL), expiresAt 동기화
        // TODO: PortOne S2S 조회, VERIFIED 검증 + under14(J001) + A002 처리

        return IdentityResponse.NewUserComplete.mockOf();
    }
}