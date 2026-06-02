#!/usr/bin/env bash
# Stand up a local kind cluster with the full banking platform.
# Prereqs: docker, kind, kubectl, helm.
set -euo pipefail

CLUSTER=banking
NAMESPACE=banking
REGISTRY_NAME=kind-registry
REGISTRY_PORT=5001

cd "$(dirname "$0")/.."

echo "==> 1. Local image registry"
if [ "$(docker inspect -f '{{.State.Running}}' "${REGISTRY_NAME}" 2>/dev/null || true)" != "true" ]; then
  docker run -d --restart=always -p "127.0.0.1:${REGISTRY_PORT}:5000" --name "${REGISTRY_NAME}" registry:2
fi

echo "==> 2. kind cluster"
if ! kind get clusters | grep -q "^${CLUSTER}$"; then
  kind create cluster --config local-setup/kind-cluster.yaml
fi
docker network connect kind "${REGISTRY_NAME}" 2>/dev/null || true

echo "==> 3. Namespace + Pod Security Standard"
kubectl create ns "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
kubectl label ns "${NAMESPACE}" pod-security.kubernetes.io/enforce=baseline --overwrite
kubectl label ns "${NAMESPACE}" pod-security.kubernetes.io/warn=restricted --overwrite

echo "==> 4. Ingress controller (nginx)"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s

echo "==> 5. Helm repos"
helm repo add hashicorp https://helm.releases.hashicorp.com >/dev/null
helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts >/dev/null
helm repo update >/dev/null

echo "==> 6. Build + push service images to local registry"
for svc in config-server discovery-server api-gateway auth-service customer-service account-service transaction-service notification-service workflow-service; do
  IMAGE="localhost:${REGISTRY_PORT}/banking/${svc}:dev"
  docker build --build-arg SERVICE="${svc}" -t "${IMAGE}" .
  docker push "${IMAGE}"
done

echo "==> 7. helm dependency update"
helm dependency update helm/banking-platform/

echo "==> 8. Install platform"
helm upgrade --install banking helm/banking-platform/ \
  -n "${NAMESPACE}" \
  -f helm/banking-platform/values-local.yaml \
  --wait --timeout 15m

echo "==> 9. Waiting on Vault bootstrap Job"
kubectl -n "${NAMESPACE}" wait --for=condition=complete \
  job/banking-vault-bootstrap --timeout=300s || true

echo ""
echo "==> Done. Try:"
echo "   kubectl -n ${NAMESPACE} get pods"
echo "   curl http://api.bank.localtest.me:8080/actuator/health"
