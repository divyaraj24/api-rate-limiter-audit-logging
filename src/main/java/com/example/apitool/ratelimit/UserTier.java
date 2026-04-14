package com.example.apitool.ratelimit;

public enum UserTier {
    FREE,
    PREMIUM;

    public static UserTier fromHeader(String value) {
        if (value == null || value.isBlank()) {
            return FREE;
        }
        return "premium".equalsIgnoreCase(value) ? PREMIUM : FREE;
    }
}
