#!/usr/bin/env bash
set -euo pipefail

echo "=== Installing ArgoCD ==="
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "Waiting for ArgoCD server to be ready..."
kubectl wait --for=condition=available deployment/argocd-server -n argocd --timeout=180s

echo ""
echo "=== Applying ArgoCD Application ==="
kubectl apply -f "$(dirname "$0")/argocd-app.yaml"

echo ""
echo "=== ArgoCD Ready ==="
echo ""
echo "  UI:       make argocd-open  (then visit https://localhost:8888)"
echo "  Username: admin"
echo "  Password: $(kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d)"
echo ""
echo "ArgoCD will sync the cluster automatically when charts/job-scanner/values.yaml changes."
