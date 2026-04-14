package com.example.apitool.ratelimit;

public record RateLimitDecision(boolean allowed, long remainingTokens, long retryAfterSeconds) {
}
