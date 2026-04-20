package com.t1.popcon.user.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.SuperLoginResponse;
import com.t1.popcon.user.dto.UserIdResponse;
import com.t1.popcon.user.dto.UserProfileResponse;
import com.t1.popcon.user.dto.UserProfileUpdateResponse;
import com.t1.popcon.user.dto.statistics.UserActivityStatisticsResponse;
import com.t1.popcon.user.service.UserHistoryService;
import com.t1.popcon.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 프로필 수정 (닉네임 / 이미지)
     * PATCH /users/me/profile
     * Content-Type: multipart/form-data
     *
     * @param nickname    변경할 닉네임 (선택)
     * @param file        업로드할 프로필 이미지 파일 (선택)
     * @param deleteImage true이면 기존 프로필 이미지 삭제 (선택, 기본값 false)
     */
    @PatchMapping(value = "/me/profile", consumes = "multipart/form-data")
    public ApiResponse<UserProfileUpdateResponse> updateMyProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String nickname,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestParam(defaultValue = "false") boolean deleteImage
    ) {
        UserProfileUpdateResponse response =
                userService.updateProfile(authUser.id(), nickname, file, deleteImage);
        return ApiResponse.ok("프로필이 수정되었습니다.", response);
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

    /**
     * 시연용 슈퍼 계정 순차 로그인
     * POST /users/login/super
     */
    @PostMapping("/login/super")
    public ApiResponse<SuperLoginResponse> loginSuperAccount() {
        SuperLoginResponse response = userService.loginSuperAccount();
        return ApiResponse.ok("시연용 계정 로그인이 완료되었습니다.", response);
    }
}
