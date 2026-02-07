package com.t1.popcon.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

	@GetMapping("/health")
	public String healthCheck() {
		return "Pop-Con Server is running";
	}
}