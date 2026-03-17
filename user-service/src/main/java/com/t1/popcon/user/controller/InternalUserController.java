package com.t1.popcon.user.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.UserSocialLookupResponse;
import com.t1.popcon.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/internal/users/social")
    public ApiResponse<UserSocialLookupResponse> findBySocial(
            @RequestParam String provider,
            @RequestParam String providerUserId
    ) {
        return ApiResponse.ok(userService.findBySocial(provider, providerUserId));
    }
}