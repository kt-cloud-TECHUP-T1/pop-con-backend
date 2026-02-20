package com.t1.popcon.auth.service;

import com.t1.popcon.auth.dto.AuthRequest;
import com.t1.popcon.auth.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	public AuthResponse.Signup signup(Long userId, AuthRequest.Signup request) {

		log.info("회원가입 요청 도달 - userId: {}, request: {}", userId, request);

		return AuthResponse.Signup.mockOf();
	}
}