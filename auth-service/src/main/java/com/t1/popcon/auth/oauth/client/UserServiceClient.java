package com.t1.popcon.auth.oauth.client;

import com.t1.popcon.auth.oauth.client.dto.UserSocialLookupApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "userServiceClient",
        url = "${services.user-service.url}"
)
public interface UserServiceClient {

    @GetMapping("/internal/users/social")
    UserSocialLookupApiResponse findBySocial(
            @RequestParam("provider") String provider,
            @RequestParam("providerUserId") String providerUserId
    );
}