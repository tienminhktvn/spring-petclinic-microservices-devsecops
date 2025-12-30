#!/bin/bash

TARGET_URL="${1:-http://192.168.195.115:32424}"
REPORT_DIR="./zap-reports"

mkdir -p $REPORT_DIR

echo "🔍 Starting OWASP ZAP API Scan..."

# Scan specific API endpoints
ENDPOINTS=(
  "/api/customer/owners"
  "/api/vet/vets"
  "/api/visit/pets/visits?petId=1"
  "/api/gateway/owners/1"
)

for endpoint in "${ENDPOINTS[@]}"; do
  echo "Scanning: ${TARGET_URL}${endpoint}"
  curl -s "${TARGET_URL}${endpoint}" > /dev/null
done

# Run ZAP with API scan
docker run --rm -v $(pwd)/$REPORT_DIR:/zap/wrk:rw \
  --network host \
  -t zaproxy/zap-stable zap-baseline.py \
  -t $TARGET_URL \
  -r zap-api-report.html \
  -J zap-api-report.json \
  -I

echo "✅ API scan complete!"