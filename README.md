# Job Scanner Platform

A multi-tenant AI-powered job scanner SaaS backend. Each tenant configures search criteria, the platform discovers matching jobs via pluggable providers, an AI scorer ranks them against the tenant's career profile, and results are served through a unified API gateway.

## Architecture

```
api-gateway   (port 8090) — single entry point: JWT validation, routing, per-tenant rate limiting
auth-service  (port 8080) — tenant signup, login, JWT issuance
scan-service  (port 8081) — search configs, job discovery, Kafka producer
scoring-service (port 8082) — Kafka consumer, AI scoring, Redis LLM cache, Kafka producer
search-service  (port 8083) — indexes scored jobs, serves tenant-scoped search & results
```

All services enforce multi-tenant isolation at the persistence/query layer via `TenantContext` (ThreadLocal UUID). No query — database or search — can cross tenant boundaries by construction.

### Data flow

```
scan-service → [job-discovered] → scoring-service → [job-scored] → search-service
```

---

## Gateway — Single Entry Point

**All client traffic goes through `http://localhost:8090`** (or the gateway's public address).
The gateway validates JWTs, injects `X-Tenant-Id` / `X-User-Id` headers, and applies per-tenant rate limiting.

### Route table

| Path prefix | Downstream | Auth required |
|---|---|---|
| `POST /api/v1/auth/signup` | auth-service:8080 | No |
| `POST /api/v1/auth/login` | auth-service:8080 | No |
| `/api/v1/jobs/**` | search-service:8083 | Yes |
| `/api/v1/scans/**` | scan-service:8081 | Yes |
| `/api/v1/scores/**` | scoring-service:8082 | Yes |
| `/actuator/**` | gateway | No |

**Rate limiting**: 20 req/s per tenant (burst 40), backed by Redis. Returns `429` when exceeded. Public routes rate-limited by IP.

---

## Quick Start — Full Mock Mode (zero external accounts)

```bash
git clone https://github.com/donny-mathew/job-scanner-platform.git
cd job-scanner-platform

APP_JOB_PROVIDER=mock APP_SCORER=mock APP_SEARCH_INDEX=memory \
  docker-compose up --build
```

Wait ~90s for all services to start. Mock mode: jobs are canned data, scores are always 75, search uses an in-memory index (no OpenSearch required).

---

## Full Pipeline Walkthrough (via gateway)

### 1. Sign up and get a JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8090/api/v1/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"securepass123","tenantName":"my-company"}' \
  | jq -r .token)

echo "Token: $TOKEN"
```

### 2. Set your scoring profile

```bash
curl -X PUT http://localhost:8090/api/v1/scores/scoring-profile \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"profileText":"Java/Spring Boot lead engineer, 8 years experience, seeking AU visa sponsorship"}'
```

### 3. Create a search config

```bash
curl -X POST http://localhost:8090/api/v1/scans/search-configs \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"keywords":["java","spring boot"],"location":"Australia","enabled":true}'
```

### 4. Trigger a scan

```bash
curl -X POST http://localhost:8090/api/v1/scans/run \
  -H "Authorization: Bearer $TOKEN"
```

The scan discovers jobs → publishes `job-discovered` to Kafka → scoring-service scores them → publishes `job-scored` → search-service indexes them.

### 5. Search your scored jobs

```bash
# Full-text search, sorted by score desc
curl "http://localhost:8090/api/v1/jobs/search?q=java&location=Sydney&minScore=70" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Get a specific job by id
curl "http://localhost:8090/api/v1/jobs/{id}" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## Health Checks

```bash
curl http://localhost:8090/actuator/health   # gateway
curl http://localhost:8080/actuator/health   # auth-service
curl http://localhost:8081/actuator/health   # scan-service
curl http://localhost:8082/actuator/health   # scoring-service
curl http://localhost:8083/actuator/health   # search-service
```

---

## Switching to Real Providers

### Real job listings (Apify)

```bash
APP_JOB_PROVIDER=apify \
APIFY_TOKEN=your_token \
APIFY_ACTOR_ID=your_actor_id \
APP_SCORER=mock \
APP_SEARCH_INDEX=memory \
docker-compose up --build
```

### Real AI scoring (Anthropic)

```bash
APP_JOB_PROVIDER=mock \
APP_SCORER=anthropic \
ANTHROPIC_API_KEY=your_key \
ANTHROPIC_MODEL=claude-haiku-4-5-20251001 \
APP_SEARCH_INDEX=memory \
docker-compose up --build
```

### Real OpenSearch index

```bash
APP_SEARCH_INDEX=opensearch \
docker-compose up --build
```

OpenSearch runs in the Docker Compose stack at port 9200 with security disabled for local use.

### Full production mode

```bash
APP_JOB_PROVIDER=apify \
APIFY_TOKEN=your_token \
APIFY_ACTOR_ID=your_actor_id \
APP_SCORER=anthropic \
ANTHROPIC_API_KEY=your_key \
APP_SEARCH_INDEX=opensearch \
JWT_SECRET=your-secret-min-32-chars \
docker-compose up --build
```

---

## Running Tests

```bash
# auth-service
cd auth-service && mvn test

# scan-service (unit + integration — needs Docker for Testcontainers)
cd scan-service && mvn test

# scoring-service (unit + integration — needs Docker for Testcontainers)
cd scoring-service && mvn test

# search-service (unit + isolation tests — no Docker needed, uses in-memory index)
cd search-service && mvn test

# api-gateway (unit + routing tests)
cd api-gateway && mvn test
```

---

## Services Overview

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| api-gateway | 8090 | — | JWT validation, routing, rate limiting |
| auth-service | 8080 | authdb | Tenant management, user auth, JWT |
| scan-service | 8081 | scandb | Search configs, job discovery |
| scoring-service | 8082 | scoringdb | AI scoring, Redis cache |
| search-service | 8083 | — (OpenSearch) | Job search, results |

### Kafka Topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `job-discovered` | scan-service | scoring-service |
| `job-scored` | scoring-service | search-service |

---

## Phase 4 — Local Kubernetes (kind + Helm)

Run the entire platform on a local `kind` cluster with a single command. No cloud account required; all adapters default to mocks.

### Prerequisites

```bash
brew install kind helm kubectl
```

### Start everything

```bash
make up
```

This creates a `kind` cluster, builds all 5 Docker images, loads them into kind, and deploys the Helm chart. The API gateway is accessible at `http://localhost:8090` via a NodePort → hostPort mapping.

### Verify

```bash
make status        # kubectl get pods — all should be Running
make smoke         # end-to-end: signup → scan → search
```

### Tear down

```bash
make down          # deletes the kind cluster
```

### Individual targets

| Target | What it does |
|--------|-------------|
| `make cluster` | Create kind cluster only |
| `make build` | Build all 5 Docker images |
| `make load` | Load images into kind (no registry needed) |
| `make deploy` | Helm install/upgrade |
| `make status` | `kubectl get pods` |
| `make smoke` | End-to-end smoke test |

### Switching adapters in k8s

```bash
helm upgrade --install job-scanner charts/job-scanner \
  --set scanService.jobProvider=apify \
  --set scanService.apifyToken=your_token \
  --set scoringService.scorer=anthropic \
  --set scoringService.anthropicApiKey=your_key \
  --set searchService.searchIndex=opensearch
```

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes (prod) | `change-me-to-a-secure-secret-of-at-least-32-chars` | HS256 signing key |
| `APP_JOB_PROVIDER` | No | `mock` | `mock` or `apify` |
| `APP_SCORER` | No | `mock` | `mock` or `anthropic` |
| `APP_SEARCH_INDEX` | No | `memory` | `memory` or `opensearch` |
| `APIFY_TOKEN` | If apify | — | Apify API token |
| `APIFY_ACTOR_ID` | If apify | — | Apify actor to run |
| `ANTHROPIC_API_KEY` | If anthropic | — | Anthropic API key |
| `ANTHROPIC_MODEL` | No | `claude-haiku-4-5-20251001` | Claude model to use |
| `RATE_LIMIT_REPLENISH_RATE` | No | `20` | Requests per second per tenant |
| `RATE_LIMIT_BURST_CAPACITY` | No | `40` | Burst allowance per tenant |
