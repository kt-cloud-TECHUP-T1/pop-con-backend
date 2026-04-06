package com.t1.popcon.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_PREFIX = "/internal/";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Value("${internal.api-secret:changeme-local-secret}")
    private String internalSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String receivedSecret = request.getHeader(INTERNAL_SECRET_HEADER);

        if (receivedSecret == null || !internalSecret.equals(receivedSecret)) {
            log.warn("내부 API 인증 실패 - uri: {}, method: {}, remoteAddr: {}",
                    request.getRequestURI(), request.getMethod(), request.getRemoteAddr());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Unauthorized internal request\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
