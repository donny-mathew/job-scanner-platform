# Job Scanner Platform

A multi-tenant AI-powered job scanner SaaS backend. Each tenant configures search criteria, the platform discovers matching jobs via pluggable providers, and an AI scorer ranks them against the tenant's career profile.

## Architecture

```
auth-service  (port 8080) — tenant signup, login, JWT issuance
scan-service  (port 8081) — search configs, job discovery, Kafka producer
scoring-service (port 8082) — Kafka consumer, AI scoring, Redis cache
```

All services enforce multi-tenant isolation at the persistence layer via `TenantContext` (ThreadLocal UUID). No query can cross tenant boundaries.

---

## Quick Start (Mock Mode)

Mock mode uses no external APIs — jobs are canned data, scores are always 75.

```bash
git clone https://github.com/donny-mathew/job-scanner-platform.git
cd job-scanner-platform

APP_JOB_PROVIDER=mock APP_SCORER=mock docker-compose up --build
```

Wait for all services to report healthy (takes ~60s on first build).

---

## Full Pipeline Walkthrough

### 1. Sign up and get a JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"securepass123","tenantName":"my-company"}' \
  | jq -r .token)

echo "Token: $TOKEN"
```

### 2. Set your scoring profile

```bash
curl -X PUT http://localhost:8082/api/v1/scoring-profile \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"profileText":"Java/Spring Boot lead engineer, 8 years experience, seeking AU visa sponsorship"}'
```

### 3. Create a search config

```bash
curl -X POST http://localhost:8081/api/v1/search-configs \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "keywords": ["java", "spring boot"],
    "location": "Australia",
    "enabled": true
  }'
```

### 4. Trigger a scan

```bash
curl -X POST http://localhost:8081/api/v1/scans/run \
  -H "Authorization: Bearer $TOKEN"
```

The scan discovers jobs, persists them, and publishes `job-discovered` events to Kafka. The scoring-service consumes these events and scores each job asynchronously (within a few seconds in mock mode).

### 5. Check your scores

```bash
curl http://localhost:8082/api/v1/job-scores \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## Health Checks

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

---

## Switching to Real Providers

### Apify (real job listings)

```bash
APP_JOB_PROVIDER=apify \
APIFY_TOKEN=your_token \
APIFY_ACTOR_ID=your_actor_id \
APP_SCORER=mock \
docker-compose up --build
```

### Anthropic Claude (real AI scoring)

```bash
APP_JOB_PROVIDER=mock \
APP_SCORER=anthropic \
ANTHROPIC_API_KEY=your_key \
ANTHROPIC_MODEL=claude-haiku-4-5-20251001 \
docker-compose up --build
```

### Full production mode

```bash
APP_JOB_PROVIDER=apify \
APIFY_TOKEN=your_token \
APIFY_ACTOR_ID=your_actor_id \
APP_SCORER=anthropic \
ANTHROPIC_API_KEY=your_key \
JWT_SECRET=your-secret-min-32-chars \
docker-compose up --build
```

---

## Running Tests

```bash
# scan-service unit + integration tests (requires Docker for Testcontainers)
cd scan-service && mvn test

# scoring-service unit + integration tests
cd scoring-service && mvn test

# auth-service
cd auth-service && mvn test
```

---

## Services Overview

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| auth-service | 8080 | authdb | Tenant management, user auth, JWT |
| scan-service | 8081 | scandb | Search configs, job discovery |
| scoring-service | 8082 | scoringdb | AI scoring, Redis cache |

### Kafka Topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `job-discovered` | scan-service | scoring-service |

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes (prod) | `change-me-to-a-secure-secret-of-at-least-32-chars` | HS256 signing key |
| `APP_JOB_PROVIDER` | No | `mock` | `mock` or `apify` |
| `APP_SCORER` | No | `mock` | `mock` or `anthropic` |
| `APIFY_TOKEN` | If apify | — | Apify API token |
| `APIFY_ACTOR_ID` | If apify | — | Apify actor to run |
| `ANTHROPIC_API_KEY` | If anthropic | — | Anthropic API key |
| `ANTHROPIC_MODEL` | No | `claude-haiku-4-5-20251001` | Claude model to use |
