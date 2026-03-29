package com.t1.popcon.draw.client;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.client.dto.UserInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
	name = "user-service",
	url = "${services.user-service.url:http://localhost:8081}"
)
public interface UserServiceClient {

	@GetMapping("/internal/users/{userId}")
	ApiResponse<UserInternalResponse> getUserInternal(@PathVariable("userId") Long userId);
}
