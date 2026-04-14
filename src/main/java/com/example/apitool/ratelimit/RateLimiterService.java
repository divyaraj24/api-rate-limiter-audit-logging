package com.example.apitool.ratelimit;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final String LUA_SCRIPT = """
            local tokensKey = KEYS[1]
            local timestampKey = KEYS[2]

            local capacity = tonumber(ARGV[1])
            local refillTokens = tonumber(ARGV[2])
            local refillMillis = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])

            local currentTokens = tonumber(redis.call('GET', tokensKey))
            if currentTokens == nil then
              currentTokens = capacity
            end

            local lastRefillTime = tonumber(redis.call('GET', timestampKey))
            if lastRefillTime == nil then
              lastRefillTime = now
            end

            local elapsed = now - lastRefillTime
            if elapsed > 0 then
              local intervals = math.floor(elapsed / refillMillis)
              if intervals > 0 then
                local replenished = intervals * refillTokens
                currentTokens = math.min(capacity, currentTokens + replenished)
                lastRefillTime = lastRefillTime + (intervals * refillMillis)
              end
            end

            local allowed = 0
            if currentTokens > 0 then
              allowed = 1
              currentTokens = currentTokens - 1
            end

            redis.call('SET', tokensKey, currentTokens)
            redis.call('SET', timestampKey, lastRefillTime)

            local ttl = refillMillis * 2
            redis.call('PEXPIRE', tokensKey, ttl)
            redis.call('PEXPIRE', timestampKey, ttl)

            local retryAfter = 0
            if allowed == 0 then
              retryAfter = math.ceil(refillMillis / 1000)
            end

                return tostring(allowed) .. ',' .. tostring(currentTokens) .. ',' .. tostring(retryAfter)
            """;

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
              private final DefaultRedisScript<String> tokenBucketScript;

    public RateLimiterService(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setScriptText(LUA_SCRIPT);
        this.tokenBucketScript.setResultType(String.class);
    }

    @SuppressWarnings("null")
    public RateLimitDecision consumeToken(String userId, String endpoint, UserTier tier) {
        TierRateLimitConfig config = properties.resolve(tier);
        String bucketKey = "ratelimit:" + tier.name().toLowerCase() + ":" + userId + ":" + endpoint;
        String tokensKey = bucketKey + ":tokens";
        String timestampKey = bucketKey + ":timestamp";

        long now = Instant.now().toEpochMilli();
        String result = redisTemplate.execute(
                tokenBucketScript,
                List.of(tokensKey, timestampKey),
                String.valueOf(config.getCapacity()),
                String.valueOf(config.getRefillTokens()),
                String.valueOf(config.getRefillSeconds() * 1000),
                String.valueOf(now)
        );

        if (result == null || result.isBlank()) {
            return new RateLimitDecision(false, 0, config.getRefillSeconds());
        }

        String[] parts = result.split(",");
        if (parts.length < 3) {
            return new RateLimitDecision(false, 0, config.getRefillSeconds());
        }

        long allowed = toLong(parts[0]);
        long remaining = Math.max(0, toLong(parts[1]));
        long retryAfter = Math.max(0, toLong(parts[2]));
        return new RateLimitDecision(allowed == 1, remaining, retryAfter);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.toString(value));
    }
}
