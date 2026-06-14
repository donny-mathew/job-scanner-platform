# auth-service

## Project Overview
Multi-tenant user/auth service for the job-scanner SaaS platform. Handles signup and login, issues JWTs with `tenant_id` claims, and enforces tenant data isolation at the persistence layer via a `TenantContext` ThreadLocal — no cross-tenant leakage is possible by construction.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.3.x
- **Security**: Spring Security 6, Auth0 `java-jwt` (HS256)
- **Database**: PostgreSQL 15 + Spring Data JPA + Flyway
- **Cache**: Redis (Spring Data Redis — wired for future JWT denylist)
- **Testing**: JUnit 5, Mockito, Testcontainers, AssertJ
- **Build**: Maven 3.9
- **Infra**: Docker multi-stage build, Docker Compose

## Project Structure
```
src/main/java/com/jobscanner/auth/
├── domain/
│   ├── model/          Pure Java records: Tenant, User (no JPA annotations)
│   ├── port/in/        Use case interfaces: SignUpUseCase, LoginUseCase
│   ├── port/out/       Repository interfaces: TenantRepository, UserRepository
│   └── exception/      Domain exceptions: TenantNotFoundException, etc.
├── application/service/
│   ├── AuthService     Implements use cases; BCrypt; @Transactional signup
│   └── JwtService      HS256 sign/validate; tenant_id + role claims
└── adapter/
    ├── in/web/         AuthController, DTOs, GlobalExceptionHandler
    └── out/
        ├── persistence/ JPA entities, Spring Data repos, adapter impls
        └── security/    TenantContext (ThreadLocal), JwtAuthFilter, SecurityConfig
```

## Key Conventions
- Constructor injection only — no `@Autowired` field injection
- Domain models (`Tenant`, `User`) are pure Java records; JPA entities are adapter-layer only
- `TenantContext.get()` is the single source of the current tenant ID — never pass `tenantId` as a method parameter from application layer downward
- All repository adapter methods call `TenantContext.requireTenantId()` which throws if context is missing (fail-safe)
- DTOs live in `adapter/in/web/dto/` — never pass domain models across the HTTP boundary
- Flyway migrations in `src/main/resources/db/migration/` — never modify existing migrations

## Environment Variables
| Variable | Default | Required |
|----------|---------|----------|
| `DB_HOST` | `localhost` | no |
| `DB_PORT` | `5432` | no |
| `DB_NAME` | `authdb` | no |
| `DB_USERNAME` | — | yes (prod) |
| `DB_PASSWORD` | — | yes (prod) |
| `REDIS_HOST` | `localhost` | no |
| `REDIS_PORT` | `6379` | no |
| `JWT_SECRET` | — | yes (min 32 chars) |
| `JWT_EXPIRY_HOURS` | `24` | no |

## Environment Setup

```bash
# 1. Start dependencies
docker-compose up postgres redis -d

# 2. Set required env vars
export DB_USERNAME=jobscanner
export DB_PASSWORD=secret
export JWT_SECRET=change-me-to-a-secure-32-char-secret

# 3. Run the service
./mvnw spring-boot:run

# 4. Full stack via Docker
docker-compose up --build
```

## Notes for Claude
- Always check TASK.md before starting work
- Phase approval from user required before starting each phase
- The tenant isolation test (`TenantIsolationIT`) is the acceptance criterion for Phase 1 — it must pass
- `TenantContext` must be cleared in a `finally` block / filter to avoid ThreadLocal leaks
- JWT signing key must never be hardcoded — always read from `JWT_SECRET` env var
