#!/usr/bin/env bash
set -euo pipefail
kind delete cluster --name banking || true
docker rm -f kind-registry || true
