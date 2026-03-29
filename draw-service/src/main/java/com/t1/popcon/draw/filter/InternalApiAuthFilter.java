package com.t1.popcon.draw.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_PREFIX = "/internal/";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Value("${internal.api-secret}")
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
        // 내부 API 인증 성공 시 SecurityContext 설정
        var auth = new UsernamePasswordAuthenticationToken(
          "internal-service", null,
          List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
