#!/usr/bin/env bash
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../../.." && pwd)"
run_id="${1:-$(date +%Y%m%d-%H%M%S)}"
out="$here/runs/$run_id"

for transform in add-if-zero add-if-one for-to-while deepen-while; do
  "$repo/util/robustness/run-benchmark.sh" \
    --mode preferred \
    --transform "$transform" \
    --sites all \
    --input "$here/inputs" \
    --out "$out" \
    --timeout 300
done

echo "Results: $out"
