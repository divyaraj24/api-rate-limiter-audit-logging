package com.example.apitool.web;

import com.example.apitool.ratelimit.RateLimitDecision;
import com.example.apitool.ratelimit.RateLimiterService;
import com.example.apitool.ratelimit.UserTier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(10)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_TIER_HEADER = "X-User-Tier";

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String endpoint = request.getMethod() + ":" + request.getRequestURI();
        String userId = resolveUserId(request);
        UserTier tier = UserTier.fromHeader(request.getHeader(USER_TIER_HEADER));

        RateLimitDecision decision = rateLimiterService.consumeToken(userId, endpoint, tier);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remainingTokens()));

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "error", "rate_limit_exceeded",
                    "message", "Request quota exceeded for this endpoint",
                    "userId", userId,
                    "tier", tier.name().toLowerCase(),
                    "retryAfterSeconds", decision.retryAfterSeconds()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            return "anonymous";
        }
        return userId;
    }
}
