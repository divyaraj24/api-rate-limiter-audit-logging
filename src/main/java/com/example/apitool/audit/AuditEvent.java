package com.example.apitool.audit;

import java.time.Instant;

public record AuditEvent(
        Instant timestamp,
        String userId,
        String endpoint,
        String httpMethod,
        int statusCode,
        long latencyMs,
        String clientIp,
        String requestId
) {
}
