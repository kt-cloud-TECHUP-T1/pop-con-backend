package com.t1.popcon.auth.signup.client;

import com.t1.popcon.auth.signup.client.dto.UserCreateRequest;
import com.t1.popcon.auth.signup.client.dto.UserCreateResponse;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
	name = "user-service", 
	url = "${services.user-service.url}",
	contextId = "signUpUserServiceClient"
)
public interface SignUpUserServiceClient {

	@PostMapping("/users")
	ApiResponse<UserCreateResponse> createUser(@RequestBody UserCreateRequest request);
}
