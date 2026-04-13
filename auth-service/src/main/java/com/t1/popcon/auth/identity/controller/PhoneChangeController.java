package com.t1.popcon.auth.identity.controller;

import com.t1.popcon.auth.identity.dto.PhoneChangeRequest;
import com.t1.popcon.auth.identity.dto.PhoneChangeResponse;
import com.t1.popcon.auth.identity.service.PhoneChangeService;
import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth/identity")
@RequiredArgsConstructor
public class PhoneChangeController {

    private final PhoneChangeService phoneChangeService;

    /**
     * 휴대폰 번호 변경
     * - 개인정보 수정 페이지에서 본인인증 완료 후 호출
     * - JWT 필수 (로그인한 사용자만 허용)
     */
    @PostMapping("/phone-change")
    public ApiResponse<PhoneChangeResponse> changePhone(
            @Valid @RequestBody PhoneChangeRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PhoneChangeResponse response = phoneChangeService.changePhone(authUser.id(), request);
        return ApiResponse.ok("휴대폰 번호가 성공적으로 변경되었습니다.", response);
    }
}
