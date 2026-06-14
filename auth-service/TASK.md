# TASK.md — job-scanner-platform / auth-service

## 🎯 Goal
Build a production-grade multi-tenant auth service (Phase 1 of the job-scanner SaaS platform) that proves tenant isolation at the persistence layer — every JWT carries a `tenant_id` claim and all repository queries are automatically scoped to the calling tenant's context.

---

## 📋 Active Tasks

### Phase 1 — Auth Service + Multi-Tenancy Foundation

| Status | Task | Notes |
|--------|------|-------|
| ✅ | Scaffold Maven project + `pom.xml` | Spring Boot 3.3.x parent, all deps |
| ✅ | Write `application.yml` | Env-var placeholders (12-factor) |
| ✅ | Write Flyway migrations | V1 tenants table, V2 users table |
| ✅ | Implement domain models | `Tenant`, `User` records + domain exceptions |
| ✅ | Implement port interfaces | `SignUpUseCase`, `LoginUseCase`, `TenantRepository`, `UserRepository` |
| ✅ | Implement `JwtService` | HS256, `tenant_id` claim, key from `JWT_SECRET` env var |
| ✅ | Implement `AuthService` | Signup + login, BCrypt, `@Transactional` |
| ✅ | Implement persistence adapter | JPA entities, Spring Data repos, adapters |
| ✅ | Implement `TenantContext` + `JwtAuthFilter` + `SecurityConfig` | ThreadLocal tenant context, stateless |
| ✅ | Implement `AuthController` + DTOs + `GlobalExceptionHandler` | POST /api/v1/auth/signup, login |
| ✅ | Wire Spring `@Configuration` beans | Bind ports to adapters via component scan |
| ✅ | Write unit tests | `JwtServiceTest`, `AuthServiceTest` (Mockito) |
| ✅ | Write integration test `TenantIsolationIT` | Testcontainers Postgres — two tenants, assert data isolation |
| ✅ | Write `Dockerfile` + `docker-compose.yml` | Multi-stage build; service + postgres + redis |
| ✅ | Write `README.md` | Run instructions, curl examples, test instructions |

---

## 🗺️ Platform Roadmap

### Phase 2 — Scan + AI Scoring Pipeline ✅
| Status | Task |
|--------|------|
| ✅ | `scan-service`: search configs, job discovery, Kafka producer |
| ✅ | `scoring-service`: Kafka consumer, AI scoring (mock + Anthropic) |
| ✅ | Redis LLM response cache (SHA-256 keyed, 7-day TTL) |
| ✅ | Root `docker-compose.yml` wiring all five containers |
| ✅ | `infra/postgres/init-multiple-dbs.sh` (authdb, scandb, scoringdb) |
| ✅ | Multi-tenant isolation integration tests (Testcontainers) |
| ✅ | Root `README.md` with full pipeline run instructions |

### Phase 3 — Search/Results Service + Gateway (future)
| Status | Task |
|--------|------|
| ⚪ | OpenSearch service for job result indexing and search |
| ⚪ | Spring Cloud Gateway with per-tenant rate limiting |
| ⚪ | JWT propagation across services |

### Phase 4 — Local kind Cluster + Helm (future)
| Status | Task |
|--------|------|
| ⚪ | Helm charts for all services |
| ⚪ | Deploy to local kind cluster |
| ⚪ | ConfigMaps + Secrets for environment config |

### Phase 5 — GitOps: ArgoCD + GitHub Actions (future)
| Status | Task |
|--------|------|
| ⚪ | ArgoCD app manifests per service |
| ⚪ | GitHub Actions CI pipeline (build → push image → sync ArgoCD) |

### Phase 6 — AWS/EKS via Terraform (future)
| Status | Task |
|--------|------|
| ⚪ | Terraform: RDS, ElastiCache, EKS node groups |
| ⚪ | Deploy platform to EKS — demo + destroy |

### Phase 7 — Production Polish (future)
| Status | Task |
|--------|------|
| ⚪ | HPA (CPU + custom metrics) |
| ⚪ | Prometheus + Grafana dashboards |
| ⚪ | IRSA for AWS service access |
| ⚪ | Structured JSON logging (Logback) |

---

## ✅ Completed

- ✅ Phase 1 — Auth Service + Multi-Tenancy Foundation (all 15 tasks)
