#!/bin/bash

TARGET_URL="${1:-http://192.168.195.115:32424}"
REPORT_DIR="./zap-reports"
REPORT_NAME="zap-report-$(date +%Y%m%d-%H%M%S)"

mkdir -p $REPORT_DIR

echo "🔍 Starting OWASP ZAP Baseline Scan..."
echo "Target: $TARGET_URL"

# Run ZAP Baseline Scan (passive scan - quick)
docker run --rm -v $(pwd)/$REPORT_DIR:/zap/wrk:rw \
  -t zaproxy/zap-stable zap-baseline.py \
  -t $TARGET_URL \
  -r ${REPORT_NAME}.html \
  -J ${REPORT_NAME}.json \
  -I

echo "✅ Baseline scan complete!"
echo "Reports saved to: $REPORT_DIR/"