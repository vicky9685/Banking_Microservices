# Local setup

Two paths: `docker-compose` for fastest feedback, `kind + Helm` for parity with production manifests.

## Path A — docker-compose (laptop, fast)

Prereqs: Docker Desktop 4.x, JDK 21 (only if you want to use `mvn` directly).

```bash
make compose-up
```

Services come up in dependency order:
- Postgres → Vault → Vault bootstrap (seeds policies + KV) → Kafka → Solace → Config + Discovery → app services → UI

Then:
- UI:           http://localhost:3000
- Gateway:      http://localhost:8080
- Eureka:       http://localhost:8761
- Vault UI:     http://localhost:8200  (token: `root`)
- Solace SEMP:  http://localhost:8008  (admin/admin)

Tear down (incl. volumes):
```bash
make compose-down
```

## Path B — kind + Helm (Kubernetes parity)

Prereqs: docker, kind, kubectl, helm 3.x.

```bash
make kind-up
```

Script does:
1. Boots a local Docker registry on `localhost:5001`.
2. Creates a 3-node kind cluster (`banking`) with ingress port mappings.
3. Labels the namespace with Pod Security Standards (`baseline` enforce + `restricted` warn).
4. Installs ingress-nginx.
5. Builds every service image and pushes to the local registry.
6. `helm dependency update` (pulls Postgres, Redis, Kafka, Vault).
7. `helm upgrade --install banking helm/banking-platform/ -f values-local.yaml`.
8. Waits on the Vault bootstrap Job which seeds KV + K8s auth roles.

Once green:
```bash
kubectl -n banking get pods
curl http://api.bank.localtest.me:8080/actuator/health
```

Tear down:
```bash
make kind-down
```

## What just happened — Vault flow

1. Helm renders a `ServiceAccount` per service (e.g. `banking-account-service-sa`).
2. The Vault bootstrap Job runs once and:
   - Enables KV v2 at `secret/`.
   - Enables the `kubernetes` auth method.
   - For each service: writes a Vault policy granting read on `secret/banking/<svc>` and `secret/banking/common`.
   - Creates a Vault role binding `auth/kubernetes/role/<svc>` → ServiceAccount.
   - Seeds initial KV values for `db.username`, `db.password`, `solace.username`, `solace.password`, `jwt.signing-key`.
3. Each app pod starts and Spring Cloud Vault uses the mounted ServiceAccount token to authenticate, then reads its KV path.
4. `bootstrap.yml` placeholders like `${db.password}` resolve before Spring's main context starts.

If you ever want to inspect what an app sees:
```bash
kubectl exec deploy/banking-account-service -- env | grep -i db
# nothing! Secrets are never put in env vars.
kubectl exec deploy/banking-account-service -- \
  wget -qO- http://localhost:8082/actuator/health
```

## UI

The UI is served by nginx on port 3000 (compose) and proxies `/api/*` to `api-gateway:8080`. In K8s, use the ingress at `api.bank.localtest.me` and serve the static bundle from any web layer (CDN, ingress, separate Deployment).

Dev mode (hot reload):
```bash
cd ui && npm install && npm run dev
# UI on http://localhost:3000, proxies /api to http://localhost:8080
```
