# job-scanner-platform

## Project Overview
Multi-tenant AI-powered job scanner SaaS backend. Five Spring Boot 3.x microservices communicate via Kafka and are deployed locally via kind + Helm. Tenant isolation is enforced at every persistence boundary — structurally impossible to leak cross-tenant data.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.3.5, Spring Cloud Gateway (WebFlux)
- **Security**: Auth0 java-jwt (HS256), Spring Security 6
- **Databases**: PostgreSQL 15 (auth/scan/scoring), OpenSearch 2.13 (search), Redis 7 (rate limiting, LLM cache)
- **Messaging**: Apache Kafka 7.4 (Confluent images)
- **Build**: Maven 3.9, Docker multi-stage layered jars
- **Local k8s**: kind, Helm 3

## Phase Status
- Phase 1 ✅ auth-service — tenant signup/login, JWT
- Phase 2 ✅ scan-service + scoring-service — job discovery, AI scoring, Kafka pipeline
- Phase 3 ✅ search-service + api-gateway — search, JWT validation, rate limiting
- Phase 4 ✅ kind + Helm — local k8s deployment
- Phase 5 ⚪ ArgoCD + GitHub Actions CI
- Phase 6 ⚪ AWS/EKS via Terraform
- Phase 7 ⚪ HPA, Prometheus/Grafana, structured logging

## Service Ports
| Service | Port |
|---------|------|
| api-gateway | 8090 |
| auth-service | 8080 |
| scan-service | 8081 |
| scoring-service | 8082 |
| search-service | 8083 |

## Key Conventions
- Hexagonal architecture in every service: `domain/`, `application/`, `adapter/`
- `TenantContext` (ThreadLocal) — single source of current tenant; set in filters, cleared in finally
- All repository adapters call `TenantContext.requireTenantId()` — throws if unset
- Constructor injection only; no `@Autowired` field injection
- `@ConditionalOnProperty` for adapter selection (mock vs real)
- Domain models are pure Java records; JPA entities are adapter-layer only
- API gateway validates JWT and injects `X-Tenant-Id` + `X-User-Id` downstream
- Kafka k8s uses single PLAINTEXT listener at `kafka:9092` (docker-compose uses dual-listener at 29092)

## Running Locally

### Docker Compose (fastest, zero external accounts)
```bash
APP_JOB_PROVIDER=mock APP_SCORER=mock APP_SEARCH_INDEX=memory \
  docker-compose up --build
```

### Local Kubernetes (kind)
```bash
make up      # cluster + build + load + deploy
make status  # check pods
make smoke   # end-to-end test
make down    # tear down
```

## Notes for Claude
- Always check TASK.md before starting work
- User approves whole phases, not individual tasks — stop for approval at phase boundaries only
- Phase 4 Helm chart at `charts/job-scanner/`; values.yaml controls all adapter switches
- `imagePullPolicy: IfNotPresent` so kind uses locally loaded images (not pulling from registry)
- search-service OPENSEARCH_PASSWORD reuses `db-password` secret key in default values (admin/admin for local OpenSearch) — production should use a separate secret
