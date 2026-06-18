CLUSTER_NAME = job-scanner
CHART_PATH   = charts/job-scanner
RELEASE_NAME = job-scanner
NAMESPACE    = default
SERVICES     = auth-service scan-service scoring-service search-service api-gateway

.PHONY: up down cluster build load deploy status smoke argocd-setup argocd-open

up: cluster build load deploy

down:
	kind delete cluster --name $(CLUSTER_NAME)

cluster:
	kind create cluster --name $(CLUSTER_NAME) --config kind-config.yaml

build:
	$(foreach svc,$(SERVICES),docker build -t $(svc):latest ./$(svc);)

load:
	$(foreach svc,$(SERVICES),kind load docker-image $(svc):latest --name $(CLUSTER_NAME);)

deploy:
	helm upgrade --install $(RELEASE_NAME) $(CHART_PATH) \
	  --namespace $(NAMESPACE) --create-namespace \
	  --set jwtSecret="change-me-to-a-secure-secret-of-at-least-32-chars" \
	  --wait --timeout 10m

status:
	kubectl get pods -n $(NAMESPACE)

smoke:
	./scripts/smoke-test.sh

argocd-setup:
	bash gitops/setup.sh

argocd-open:
	kubectl port-forward svc/argocd-server -n argocd 8888:443
