package com.t1.popcon.user.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.statistics.UserActivityStatisticsResponse;
import com.t1.popcon.user.service.UserHistoryService;
import com.t1.popcon.user.service.UserService;
import com.t1.popcon.user.dto.UserIdResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserHistoryService userHistoryService;

    /**
     * 내 프로필 조회
     * GET /users/me
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal AuthUser authUser) {
        UserProfileResponse response = userService.getUserProfile(authUser.id());
        return ApiResponse.ok("프로필 조회를 완료했습니다.", response);
    }

    @GetMapping("/me/id")
    public ApiResponse<UserIdResponse> getMyId(
      @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.ok(new UserIdResponse(authUser.id()));
    }

    /**
     * 내 활동 통계 조회
     * GET /users/me/statistics
     */
    @GetMapping("/me/statistics")
    public ApiResponse<UserActivityStatisticsResponse> getMyStatistics(@AuthenticationPrincipal AuthUser authUser) {
        UserActivityStatisticsResponse response = userHistoryService.getMyStatistics(authUser.id());
        return ApiResponse.ok("활동 통계 조회를 완료했습니다.", response);
    }
}
