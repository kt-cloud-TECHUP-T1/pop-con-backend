package com.t1.popcon.auth.signup.client;

import com.t1.popcon.auth.client.user.config.FeignClientConfig;
import com.t1.popcon.auth.signup.client.dto.UserCreateRequest;
import com.t1.popcon.auth.signup.client.dto.UserCreateResponse;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
	name = "user-service", 
	url = "${services.user-service.url}",
	configuration = FeignClientConfig.class,
	contextId = "signUpUserServiceClient"
)
public interface SignUpUserServiceClient {

	@PostMapping("/internal/users")
	ApiResponse<UserCreateResponse> createUser(@RequestBody UserCreateRequest request);
}
