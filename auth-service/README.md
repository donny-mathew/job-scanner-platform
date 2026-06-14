# auth-service

Multi-tenant authentication service for the job-scanner platform. Issues JWTs with `tenant_id` claims and enforces per-tenant data isolation at the repository layer — cross-tenant data leakage is structurally impossible.

## Prerequisites

- Docker & Docker Compose
- Java 17 + Maven 3.9 (for local dev / running tests)

## Run locally (Docker Compose)

```bash
cd auth-service

# Set a real JWT secret (or export it)
export JWT_SECRET="change-me-to-a-secure-secret-of-at-least-32-chars"

docker-compose up --build
```

Service starts on `http://localhost:8080`. Postgres and Redis start first; the service waits for their health checks.

## API

### Sign up — new tenant (auto-created)

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123","tenantName":"acme"}' | jq .
```

Response `201`:
```json
{
  "userId": "...",
  "tenantId": "...",
  "tenantName": "acme",
  "token": "<jwt>"
}
```

### Sign up — join existing tenant

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"bob@example.com","password":"password123","tenantId":"<tenantId>"}' | jq .
```

### Login

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123","tenantId":"<tenantId>"}' | jq .
```

Response `200`:
```json
{
  "token": "<jwt>",
  "tenantId": "...",
  "userId": "..."
}
```

### Verify JWT contains tenant_id

```bash
# Decode the payload (base64)
echo "<token>" | cut -d. -f2 | base64 -d 2>/dev/null | jq .
```

### Health check

```bash
curl http://localhost:8080/actuator/health
```

## Run tests

```bash
# Unit tests only (no Docker required)
./mvnw test -Dtest="**/unit/**"

# All tests including TenantIsolationIT (requires Docker for Testcontainers)
./mvnw test
```

`TenantIsolationIT` spins up a real Postgres container and proves that:
- Tenant A cannot see Tenant B's users
- Tenant B cannot see Tenant A's users
- The same email address can exist in two different tenants without conflict

## Environment variables

| Variable | Default | Required |
|----------|---------|----------|
| `DB_HOST` | `localhost` | no |
| `DB_PORT` | `5432` | no |
| `DB_NAME` | `authdb` | no |
| `DB_USERNAME` | — | **yes** |
| `DB_PASSWORD` | — | **yes** |
| `REDIS_HOST` | `localhost` | no |
| `REDIS_PORT` | `6379` | no |
| `JWT_SECRET` | — | **yes** (min 32 chars) |
| `JWT_EXPIRY_HOURS` | `24` | no |

## Project structure

```
src/main/java/com/jobscanner/auth/
├── domain/             Pure domain — no framework dependencies
│   ├── model/          Tenant, User (Java records)
│   ├── port/in/        SignUpUseCase, LoginUseCase
│   ├── port/out/       TenantRepository, UserRepository
│   └── exception/      Domain exceptions
├── application/service/
│   ├── AuthService     Signup + login orchestration
│   └── JwtService      JWT sign / validate
└── adapter/
    ├── in/web/         AuthController, DTOs, GlobalExceptionHandler
    └── out/
        ├── persistence/ JPA entities, Spring Data repos, adapters
        └── security/    TenantContext (ThreadLocal), JwtAuthFilter, SecurityConfig
```
