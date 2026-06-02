SHELL := /bin/bash

.PHONY: help build test compose-up compose-down kind-up kind-down deploy-local helm-template helm-lint scan

help:                          ## Show targets
	@grep -E '^[a-zA-Z_-]+:.*?##' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

build:                         ## Maven build all modules (skip tests)
	mvn -B -DskipTests package

test:                          ## Run all tests
	mvn -B verify

compose-up:                    ## Local stack via docker-compose (Postgres, Kafka, Solace, Vault dev, services)
	docker compose up --build -d

compose-down:                  ## Tear down docker-compose stack
	docker compose down -v

kind-up:                       ## Provision local kind cluster + Helm install
	bash local-setup/bootstrap.sh

kind-down:                     ## Delete local kind cluster
	bash local-setup/teardown.sh

deploy-local:                  ## Helm install / upgrade against current kube-context
	helm dependency update helm/banking-platform/
	helm upgrade --install banking helm/banking-platform/ \
	  -n banking --create-namespace \
	  -f helm/banking-platform/values-local.yaml

helm-template:                 ## Render manifests locally
	helm template banking helm/banking-platform/ -f helm/banking-platform/values-local.yaml

helm-lint:                     ## Lint chart
	helm lint helm/banking-platform/

scan:                          ## SCA + container scan via Trivy (requires trivy installed)
	@for svc in api-gateway auth-service customer-service account-service transaction-service notification-service; do \
	  echo "==> trivy $$svc"; \
	  trivy image --quiet --severity HIGH,CRITICAL "localhost:5001/banking/$$svc:dev" || true; \
	done
