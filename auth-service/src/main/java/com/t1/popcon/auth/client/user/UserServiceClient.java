package com.t1.popcon.auth.client.user;

import com.t1.popcon.auth.client.user.config.FeignClientConfig;
import com.t1.popcon.auth.client.user.dto.UserLookupResponse;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * user-service와 통신하는 Feign Client
 */
@FeignClient(
        name = "userServiceClient",
        url = "${services.user-service.url}",
        configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    /**
     * 소셜 로그인 정보로 사용자 조회
     */
    @GetMapping("/internal/users/social")
    ApiResponse<UserLookupResponse> findBySocial(
            @RequestParam("provider") String provider,
            @RequestParam("providerUserId") String providerUserId
    );

    /**
     * CI 해시로 사용자 조회 (본인인증 완료 후 기존 회원 확인)
     */
    @GetMapping("/internal/users/ci")
    ApiResponse<UserLookupResponse> findByCiHash(
            @RequestParam("ciHash") String ciHash
    );

    /**
     * CI 기반 소셜 계정 연결 (본인인증 완료 후 기존 회원이 소셜 로그인 연결)
     */
    @PostMapping("/internal/users/ci/link")
    ApiResponse<Void> linkSocialByCi(
            @RequestParam("ciHash") String ciHash,
            @RequestParam("provider") String provider,
            @RequestParam("providerUserId") String providerUserId
    );
}