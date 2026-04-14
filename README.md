# API Rate Limiter + Audit Logging (Spring Boot + Redis)

This project demonstrates:

- Per-user, per-endpoint token bucket rate limiting in Redis
- Configurable thresholds by user tier (`free`, `premium`) from environment variables
- Append-only audit logging for request metadata (user, endpoint, latency, status)

## Architecture

- `RateLimitingFilter`: checks each request against a Redis token bucket key:
  - Key pattern: `ratelimit:{tier}:{userId}:{METHOD:/path}`
  - Uses a Lua script for atomic refill + consume operations
- `AuditLoggingFilter`: captures request metadata after response completion and pushes to async writer
- `AuditLogService`: single-writer append-only JSON lines log file (`audit.log`)

## Prerequisites

- Java 25 (latest LTS target in this project)
- Maven 3.9+
- Docker (for Redis)

## Run locally

1. Start Redis:

```bash
docker compose up -d
```

2. Export env vars (optional; defaults exist):

```bash
export RATE_LIMIT_FREE_CAPACITY=10
export RATE_LIMIT_FREE_REFILL_TOKENS=10
export RATE_LIMIT_FREE_REFILL_SECONDS=60

export RATE_LIMIT_PREMIUM_CAPACITY=50
export RATE_LIMIT_PREMIUM_REFILL_TOKENS=50
export RATE_LIMIT_PREMIUM_REFILL_SECONDS=60
```

3. Run app:

```bash
mvn spring-boot:run
```

## Test rate limiting

Free tier request:

```bash
curl -i -H "X-User-Id: u123" -H "X-User-Tier: free" http://localhost:8080/api/status
```

Premium tier request:

```bash
curl -i -H "X-User-Id: u999" -H "X-User-Tier: premium" http://localhost:8080/api/reports
```

Burst test (should eventually return `429`):

```bash
for i in {1..15}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "X-User-Id: u123" \
    -H "X-User-Tier: free" \
    http://localhost:8080/api/status
done
```

## Audit log format

Audit events are written as JSON lines to `./logs/audit.log`.

Example event:

```json
{
  "timestamp": "2026-04-14T11:20:13.010Z",
  "userId": "u123",
  "endpoint": "/api/status",
  "httpMethod": "GET",
  "statusCode": 200,
  "latencyMs": 12,
  "clientIp": "127.0.0.1",
  "requestId": "9f738..."
}
```

## Learning roadmap

1. Understand token bucket basics:
   - Bucket capacity = burst size
   - Refill rate controls sustained throughput
2. Observe key isolation:
   - Same user, different endpoint => different bucket
   - Different users, same endpoint => different bucket
3. Validate incident traceability:
   - Correlate `X-Request-Id` header with audit log entry
4. Tune limits per tier with env vars:
   - Free users stricter refill/capacity
   - Premium users larger burst and throughput

## Production considerations

- Store user tier from trusted auth/JWT claims (instead of plain headers)
- Ship audit logs to Kafka/S3/SIEM for retention and analytics
- Add cryptographic hashing/signatures for tamper evidence
- Add structured metrics (Micrometer + Prometheus)
