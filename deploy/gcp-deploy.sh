#!/usr/bin/env bash
# Deploy each Spring Boot service to Cloud Run (always-free tier eligible) on GCP.
# Cloud SQL PostgreSQL (db-f1-micro) covers the data layer; provisioned managed Kafka
# is replaced by a self-hosted single broker on a free-tier e2-micro VM.
#
# Prereqs: gcloud auth login, billing enabled, owner role on PROJECT_ID.
set -euo pipefail

PROJECT_ID="${PROJECT_ID:?set PROJECT_ID}"
REGION="${REGION:-us-central1}"             # always-free Cloud Run tier eligible region
ARTIFACT_REPO="${ARTIFACT_REPO:-banking}"
SQL_INSTANCE="${SQL_INSTANCE:-bank-pg}"
JWT_SECRET="${JWT_SECRET:?set JWT_SECRET}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

# All Spring Boot backend services
SERVICES=(config-server discovery-server api-gateway auth-service \
          customer-service account-service transaction-service notification-service workflow-service)

# APIs should be enabled once by the project administrator in Cloud Shell.
# The CI/CD deployer service account only needs resource deployment permissions.

echo "==> Artifact Registry repo"
gcloud artifacts repositories create "$ARTIFACT_REPO" \
  --repository-format=docker --location="$REGION" \
  --project="$PROJECT_ID" || true

gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

echo "==> Cloud SQL (free-tier db-f1-micro)"
gcloud sql instances create "$SQL_INSTANCE" \
  --database-version=POSTGRES_16 --tier=db-f1-micro \
  --region="$REGION" --storage-size=10GB --storage-type=HDD \
  --project="$PROJECT_ID" || true

# Provision databases for ALL microservices including workflowdb
for db in customerdb accountdb transactiondb authdb workflowdb; do
  echo "==> Creating database: $db"
  gcloud sql databases create "$db" --instance="$SQL_INSTANCE" \
    --project="$PROJECT_ID" || true
done

CONN="$(gcloud sql instances describe "$SQL_INSTANCE" --project="$PROJECT_ID" --format='value(connectionName)')"

echo "==> Build and push Java Backend images"
for svc in "${SERVICES[@]}"; do
  IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REPO}/${svc}:latest"
  echo "==> Building backend service: $svc"
  docker build --build-arg SERVICE="${svc}" -t "$IMAGE" .
  docker push "$IMAGE"
done

echo "==> Build and push React UI image"
IMAGE_UI="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REPO}/ui:latest"
docker build -t "$IMAGE_UI" ./ui
docker push "$IMAGE_UI"

echo "==> Deploy Java Backend Services to Cloud Run"
deploy_svc() {
  local svc="$1"; shift
  local image="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REPO}/${svc}:latest"
  gcloud run deploy "$svc" \
    --image="$image" --region="$REGION" --platform=managed \
    --allow-unauthenticated --memory=512Mi --cpu=1 --min-instances=0 --max-instances=2 \
    --add-cloudsql-instances="$CONN" \
    --set-env-vars="JWT_SECRET=${JWT_SECRET},DB_USER=postgres,DB_PASSWORD=${DB_PASSWORD},DB_HOST=/cloudsql/${CONN}" \
    --project="$PROJECT_ID" "$@"
}

deploy_svc config-server
deploy_svc discovery-server

CONFIG_URL="$(gcloud run services describe config-server --region "$REGION" --project "$PROJECT_ID" --format='value(status.url)')"
EUREKA_URL="$(gcloud run services describe discovery-server --region "$REGION" --project "$PROJECT_ID" --format='value(status.url)')/eureka/"

# Deploy remaining backend services pointing to config and discovery server URLs
for svc in api-gateway auth-service customer-service account-service transaction-service notification-service workflow-service; do
  deploy_svc "$svc" --set-env-vars="CONFIG_SERVER_URL=${CONFIG_URL},EUREKA_URL=${EUREKA_URL},JWT_SECRET=${JWT_SECRET},DB_USER=postgres,DB_PASSWORD=${DB_PASSWORD},DB_HOST=/cloudsql/${CONN}"
done

echo "==> Deploy React UI with Dynamic API Gateway Mapping"
GATEWAY_URL="$(gcloud run services describe api-gateway --region "$REGION" --project "$PROJECT_ID" --format='value(status.url)')"

gcloud run deploy ui \
  --image="$IMAGE_UI" --region="$REGION" --platform=managed \
  --allow-unauthenticated --memory=256Mi --cpu=0.5 --min-instances=0 --max-instances=2 \
  --set-env-vars="API_GATEWAY_URL=${GATEWAY_URL}/api/" \
  --project="$PROJECT_ID"

echo "==> Done! Platform successfully deployed."
echo "==> API Gateway URL: $GATEWAY_URL"
echo "==> React Web UI URL: $(gcloud run services describe ui --region "$REGION" --project "$PROJECT_ID" --format='value(status.url)')"
