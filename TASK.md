# TASK.md — Job Scanner Platform

## 🎯 Goal
Build a cloud-native, multi-tenant AI-powered job scanner SaaS from Phase 1 through Phase 7, deployable on EKS with ArgoCD GitOps.

---

## 📋 Active Tasks

### Phase 4 — Local Kubernetes (kind) + Helm

| Status | Task | Notes |
|--------|------|-------|
| ⚪ | Install and configure kind cluster | Single-node for local dev |
| ⚪ | Write Helm chart for each service (auth, scan, scoring, search, gateway) | One chart per service with values.yaml |
| ⚪ | Write umbrella Helm chart for full stack | Depends on all service charts |
| ⚪ | Package and deploy to kind cluster | `helm install job-scanner ./charts/job-scanner` |
| ⚪ | Verify full pipeline works in kind | Signup → scan → score → search via gateway |

### Phase 5 — ArgoCD GitOps + GitHub Actions CI

| Status | Task | Notes |
|--------|------|-------|
| ⚪ | Install ArgoCD in kind cluster | |
| ⚪ | Create ArgoCD Application manifests for each service | |
| ⚪ | Set up GitHub Actions CI pipeline | Build, test, push images on PR |
| ⚪ | Configure ArgoCD auto-sync from main branch | |
| ⚪ | Verify GitOps deploy: push code → CI → ArgoCD → cluster | |

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
