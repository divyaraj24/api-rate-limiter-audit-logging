package com.example.apitool.web;

import com.example.apitool.audit.AuditEvent;
import com.example.apitool.audit.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(100)
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    public AuditLoggingFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = System.currentTimeMillis() - start;
            String userId = request.getHeader("X-User-Id");
            if (userId == null || userId.isBlank()) {
                userId = "anonymous";
            }

            AuditEvent event = new AuditEvent(
                    Instant.now(),
                    userId,
                    request.getRequestURI(),
                    request.getMethod(),
                    response.getStatus(),
                    latencyMs,
                    resolveClientIp(request),
                    requestId
            );
            auditLogService.append(event);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
