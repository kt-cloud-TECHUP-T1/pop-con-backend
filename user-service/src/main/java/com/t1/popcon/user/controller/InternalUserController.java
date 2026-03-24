package com.t1.popcon.user.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.UserLookupResponse;
import com.t1.popcon.user.service.UserService;
import com.t1.popcon.user.dto.UserCreateRequest;
import com.t1.popcon.user.dto.UserCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 서비스 간 통신용 API
 */
@RestController
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    /**
     * 신규 회원 생성 (본인인증 완료 후 회원가입 절차)
     */
    @PostMapping("/internal/users")
    public ApiResponse<UserCreateResponse> createUser(@RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    /**
     * 소셜 로그인 정보로 사용자 조회
     */
    @GetMapping("/internal/users/social")
    public ApiResponse<UserLookupResponse> findBySocial(
            @RequestParam String provider,
            @RequestParam String providerUserId
    ) {
        return ApiResponse.ok(userService.findBySocial(provider, providerUserId));
    }

    /**
     * CI 해시로 사용자 조회 (본인인증 완료 후 기존 회원 확인)
     */
    @GetMapping("/internal/users/ci")
    public ApiResponse<UserLookupResponse> findByCiHash(
            @RequestParam String ciHash
    ) {
        return ApiResponse.ok(userService.findByCiHash(ciHash));
    }

    /**
     * CI 기반 소셜 계정 연결 (본인인증 완료 후 기존 회원이 소셜 로그인 연결)
     */
    @PostMapping("/internal/users/ci/link")
    public ApiResponse<Void> linkSocialByCi(
            @RequestParam String ciHash,
            @RequestParam String provider,
            @RequestParam String providerUserId
    ) {
        userService.linkSocialByCi(ciHash, provider, providerUserId);
        return ApiResponse.ok("소셜 계정이 연결되었습니다.");
    }
}