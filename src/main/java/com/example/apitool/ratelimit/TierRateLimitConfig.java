package com.example.apitool.ratelimit;

import jakarta.validation.constraints.Min;

public class TierRateLimitConfig {

    @Min(1)
    private long capacity;

    @Min(1)
    private long refillTokens;

    @Min(1)
    private long refillSeconds;

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public long getRefillSeconds() {
        return refillSeconds;
    }

    public void setRefillSeconds(long refillSeconds) {
        this.refillSeconds = refillSeconds;
    }
}
