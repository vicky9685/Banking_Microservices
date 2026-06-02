# GCP Free-Tier Deployment & Architectural Guide ($0/Month Budget)

> [!NOTE]
> Fully integrated with GitHub Actions WIF (Project: `avid-factor-259206`) and native Google Cloud Build. Last updated: 2026-06-02.

---

This guide outlines how to deploy the entire banking microservice platform (all 9 Java services, React UI, databases, cache, and message brokers) 100% within the **GCP Free Tier** and generous external developer plans without incurring any charges.

---

## 1. Always-Free Quotas Leveraged

*   **Cloud Run**: 2 million requests/month, 360,000 vCPU-seconds, and 180,000 GiB-seconds of memory (must use region `us-central1`, `us-east1`, or `us-west1`).
*   **Compute Engine (e2-micro VM)**: 1 always-free VM (0.25 vCPU, 1 GB RAM) located in `us-central1`, `us-east1`, or `us-west1` with 30 GB standard persistent disk.
*   **Artifact Registry**: 0.5 GiB storage free per month. Keep old image layers cleaned up!

---

## 2. Infrastructure Offloading Strategy

To remain strict on $0/month and avoid JVM memory issues, we offload resource-heavy databases and caches to external serverless free tiers:

### Option A: Fully Managed Serverless (Recommended, High Performance)
1.  **Database (PostgreSQL)**: Use [Neon.tech](https://neon.tech/) or [Supabase](https://supabase.com/).
    *   *Free Tier*: 0.5 GB storage, autoscaling/suspend, 100% free forever.
    *   *Setup*: Create a single project, grab the connection string, and run `deploy/db-init.sql` to initialize `customerdb`, `accountdb`, `transactiondb`, `authdb`, and `workflowdb`.
2.  **Cache (Redis)**: Use [Upstash Redis](https://upstash.com/).
    *   *Free Tier*: 10,000 commands/day, 100% serverless, $0 cost.
    *   *Setup*: Create a Redis database, copy the URL and password, and inject them into the API Gateway deployment parameters.
3.  **Messaging (Kafka)**: Use Upstash Serverless Kafka or host Redpanda on your free e2-micro VM (see below).

### Option B: Self-Hosted on Free e2-micro VM
If you want to run everything yourself, you can provision the e2-micro VM, but **you must configure SWAP space** since 1 GB of RAM is not enough to run Zookeeper, Kafka, Solace, Redis, and Vault concurrently.

#### Step 1: Provision Free e2-micro VM
Create the VM via GCP Console or CLI:
```bash
gcloud compute instances create banking-infra \
  --machine-type=e2-micro \
  --zone=us-central1-a \
  --image-family=debian-12 \
  --image-project=debian-cloud \
  --boot-disk-size=30GB \
  --boot-disk-type=pd-standard
```

#### Step 2: Configure 4 GB Swap Space (Crucial for Stability)
SSH into the instance and run:
```bash
# Create a 4GB swap file
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Make swap persistent across reboots
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Verify
free -h
```

#### Step 3: Install Docker and Docker Compose
```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
newgrp docker
```

#### Step 4: Boot Infrastructure
Copy `deploy/docker-compose-infra.yml` to the VM and run:
```bash
VM_IP=$(curl -s ifconfig.me) docker compose -f docker-compose-infra.yml up -d
```
This boots **Redpanda** (Kafka clone), **Redis**, and **Vault** using less than 300MB of real memory.

---

## 3. Dynamic Service Wiring & Configuration Fallbacks

We updated the Config Server `.yml` files so they do not crash if Vault is down:
*   **Dual-Mode Configuration**: Database and JWT settings automatically check if Vault credentials are loaded. If Vault is down, they gracefully fall back to Cloud Run environment variables (`DB_USER`, `DB_PASSWORD`, `JWT_SECRET`).
*   **Dynamic UI Proxy**: The React UI's Nginx configuration template dynamically resolves the target URL via `${API_GATEWAY_URL}` on boot, allowing it to route `/api/*` traffic to the Cloud Run API Gateway container.

---

## 4. Run the GCP Deployment Script

Once your infrastructure is ready (either Neon/Upstash or the e2-micro VM), run the automated deployment script:

```bash
# 1. Set environment variables
export PROJECT_ID="your-gcp-project-id"
export JWT_SECRET="your-secure-32char-jwt-secret-key"
export DB_PASSWORD="your-database-password"

# Optional overrides (e.g. if using Neon DB, point to its host)
# export SQL_INSTANCE="your-cloud-sql-instance-name"
# export REGION="us-central1"

# 2. Run the deploy script
bash deploy/gcp-deploy.sh
```

### Script Execution Overview:
1.  Enables GCP Cloud Run, Artifact Registry, and Cloud SQL APIs.
2.  Creates the Cloud SQL PostgreSQL instance (`db-f1-micro`) and databases for all 5 storage schemas.
3.  Compiles, builds, and pushes all **9 Java services** and the **React UI** to the Artifact Registry.
4.  Deploys the backend services to Cloud Run with optimized memory requests (512Mi each, min instances = 0 to scale to zero).
5.  Deploys the React UI with dynamic API Gateway binding.
6.  Outputs the final Gateway and Web UI URLs.
