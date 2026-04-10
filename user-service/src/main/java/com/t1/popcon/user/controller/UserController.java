package com.t1.popcon.user.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.UserProfileResponse;
import com.t1.popcon.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 프로필 관련 API 컨트롤러
 * - My 팝콘 페이지 / 프로필 관리 페이지에서 사용
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회
     * GET /users/me
     *
     * @param authUser JWT에서 추출된 인증 사용자
     * @return 사용자 프로필 정보
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal AuthUser authUser) {
        UserProfileResponse response = userService.getUserProfile(authUser.id());
        return ApiResponse.ok("프로필 조회를 완료했습니다.", response);
    }
}
