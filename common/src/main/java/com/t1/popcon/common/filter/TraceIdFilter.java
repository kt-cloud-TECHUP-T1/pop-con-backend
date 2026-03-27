package com.t1.popcon.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final int MAX_TRACE_ID_LENGTH = 64;
    private static final String TRACE_ID_PATTERN = "^[a-zA-Z0-9\\-]+$";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String traceId = getOrCreateTraceId(request);

            MDC.put(TRACE_ID, traceId);
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());

            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }

    private String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (isValidTraceId(traceId)) {
            return traceId;
        }

        return UUID.randomUUID().toString();
    }

    private boolean isValidTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return false;
        }

        if (traceId.length() > MAX_TRACE_ID_LENGTH) {
            return false;
        }

        return traceId.matches(TRACE_ID_PATTERN);
    }
}