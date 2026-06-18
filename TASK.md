# TASK.md — Job Scanner Platform

## 🎯 Goal
Build a cloud-native, multi-tenant AI-powered job scanner SaaS from Phase 1 through Phase 7, deployable on EKS with ArgoCD GitOps.

---

## 📋 Active Tasks

### Phase 5 — ArgoCD GitOps + GitHub Actions CI

| Status | Task | Notes |
|--------|------|-------|
| 🟡 | Create `.github/workflows/ci.yml` | test → build → push GHCR → update imageTag |
| ⚪ | Create `gitops/argocd-app.yaml` | ArgoCD Application pointing at charts/job-scanner |
| ⚪ | Create `gitops/setup.sh` | One-time ArgoCD install script |
| ⚪ | Update `Makefile` with argocd-setup / argocd-open targets | |
| ⚪ | Run `make argocd-setup` — install ArgoCD in kind cluster | |
| ⚪ | Push to main → verify CI green + GHCR images + ArgoCD syncs | |

### Phase 6 — AWS/EKS via Terraform

| Status | Task | Notes |
|--------|------|-------|
| ⚪ | Write Terraform modules: VPC, EKS, RDS (Postgres), ElastiCache (Redis), MSK (Kafka) | |
| ⚪ | Configure IRSA (IAM Roles for Service Accounts) | For least-privilege pod permissions |
| ⚪ | Deploy OpenSearch Service (AWS managed) | |
| ⚪ | Apply Terraform and verify cluster | |
| ⚪ | Deploy services via ArgoCD to EKS | |

### Phase 7 — Production Polish

| Status | Task | Notes |
|--------|------|-------|
| ⚪ | Configure HPA (Horizontal Pod Autoscaler) for each service | |
| ⚪ | Add Prometheus + Grafana stack | Metrics from Spring Boot Actuator |
| ⚪ | Configure structured JSON logging | |
| ⚪ | Add OpenSearch Dashboards | |
| ⚪ | Document runbook and SLA targets | |

---

## ✅ Completed

- ✅ **Phase 1** — Auth + tenant service (auth-service): signup, login, JWT with tenant_id claim, TenantContext isolation, Postgres, Redis denylist wired
- ✅ **Phase 2** — Scan + AI scoring: scan-service (job discovery, Kafka producer, Apify/mock providers), scoring-service (Kafka consumer, Anthropic/mock scorer, Redis LLM cache, job-scored Kafka event)
- ✅ **Phase 3** — Search/results service + API Gateway:
  - search-service: indexes scored jobs (InMemoryJobIndex for local/tests, OpenSearchJobIndex for prod), serves GET /api/v1/jobs/search and /api/v1/jobs/{id}, tenant isolation by construction
  - api-gateway: Spring Cloud Gateway, JWT validation, per-tenant rate limiting (Redis), routing to all downstream services
  - Docker Compose updated: OpenSearch, search-service, api-gateway wired
  - 31 tests passing across search-service and api-gateway
- ✅ **Phase 4** — Local Kubernetes (kind) + Helm:
  - All 5 Dockerfiles rewritten: 3-stage layered jar builds, non-root appuser, Spring Boot JarLauncher
  - server.port env-var support added to auth/scan/scoring services
  - management.health.probes.enabled=true added to all services (k8s liveness/readiness sub-paths)
  - kind-config.yaml: single-node cluster, hostPort 8090 → NodePort 30090
  - Helm umbrella chart at charts/job-scanner/: infra (postgres, redis, zookeeper, kafka, opensearch) + 5 app services with ConfigMaps, shared Secret, probes, securityContext
  - Kafka single PLAINTEXT listener at kafka:9092 (k8s DNS, no bridge port)
  - Makefile: make up/down/build/load/deploy/status/smoke
  - scripts/smoke-test.sh: signup → profile → search config → scan → poll → search
