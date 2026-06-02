#!/usr/bin/env bash
# Automated script to configure GCP Workload Identity Federation for secure, keyless GitHub Actions deployments.
#
# Prereqs: Install gcloud CLI locally and authenticate with owner/admin permissions.
# Usage:
#   export PROJECT_ID="your-gcp-project-id"
#   export GITHUB_REPO="your-github-username/your-repo-name"  # e.g., "john-doe/banking-platform"
#   bash deploy/setup-wif.sh

set -euo pipefail

PROJECT_ID="${PROJECT_ID:?set PROJECT_ID}"
GITHUB_REPO="${GITHUB_REPO:?set GITHUB_REPO in format 'owner/repo'}"
POOL_NAME="github-pool"
PROVIDER_NAME="github-provider"
SA_NAME="github-deployer"

echo "==> 1. Enabling required IAM and Resource Manager APIs"
gcloud services enable \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  sts.googleapis.com \
  cloudresourcemanager.googleapis.com --project "$PROJECT_ID"

echo "==> 2. Creating Workload Identity Pool: $POOL_NAME"
gcloud iam workload-identity-pools create "$POOL_NAME" \
  --project="$PROJECT_ID" \
  --location="global" \
  --display-name="GitHub Actions Pool" 2>/dev/null || true

echo "==> 3. Creating OIDC Workload Identity Provider: $PROVIDER_NAME"
gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_NAME" \
  --project="$PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="$POOL_NAME" \
  --display-name="GitHub Actions Provider" \
  --attribute-mapping="google.subject=assertion.subject,attribute.actor=assertion.actor,attribute.repository=assertion.repository" \
  --issuer-uri="https://token.actions.githubusercontent.com" 2>/dev/null || true

echo "==> 4. Creating Deployment Service Account: $SA_NAME"
gcloud iam service-accounts create "$SA_NAME" \
  --project="$PROJECT_ID" \
  --display-name="GitHub Actions Deployer SA" 2>/dev/null || true

SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
POOL_ID="$(gcloud iam workload-identity-pools describe "$POOL_NAME" --project="$PROJECT_ID" --location="global" --format='value(name)')"

echo "==> 5. Restricting Service Account to authenticate ONLY from GitHub Repository: $GITHUB_REPO"
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --project="$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/${POOL_ID}/attribute.repository/${GITHUB_REPO}"

echo "==> 6. Assigning required deployment permissions to Service Account"

assign_role() {
  local role="$1"
  echo "  -> Assigning role: $role"
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${SA_EMAIL}" \
    --role="$role" --quiet >/dev/null
}

# Assign roles required to compile, push, and deploy to Cloud Run
assign_role "roles/run.admin"                   # Create/update Cloud Run services
assign_role "roles/artifactregistry.writer"     # Push docker images
assign_role "roles/sqladmin.admin"              # Create databases in Cloud SQL
assign_role "roles/iam.serviceAccountUser"      # Act-as the run service accounts
assign_role "roles/logging.logWriter"           # Write deploy logs

echo "==> Setup Complete! Copy these variables into your GitHub Repository Secrets:"
echo "------------------------------------------------------------------------"
echo "GCP_PROJECT_ID: $PROJECT_ID"
echo "GCP_SERVICE_ACCOUNT: $SA_EMAIL"
echo "GCP_WORKLOAD_IDENTITY_PROVIDER: projects/$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')/locations/global/workloadIdentityPools/${POOL_NAME}/providers/${PROVIDER_NAME}"
echo "------------------------------------------------------------------------"
