#!/bin/bash
set -euo pipefail
mkdir -p security-reports
trivy fs --scanners secret,misconfig --severity UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL --skip-dirs .git,.gradle,build,src/frontend/node_modules,src/frontend/dist --exit-code 0 --format json --output security-reports/source.json .
python3 deploy/coolify/check-scan.py security-reports/source.json
