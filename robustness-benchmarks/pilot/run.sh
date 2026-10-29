#!/usr/bin/env bash
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
mode="${1:-preferred}"
transform="${2:-add-if-zero}"
repeat="${3:-1}"
seed="${4:-42}"
run_id="${5:-$(date +%Y%m%d-%H%M%S)}"
out="$here/runs/$run_id"

"$repo/util/robustness/run-benchmark.sh" \
  --mode "$mode" \
  --transform "$transform" \
  --repeat "$repeat" \
  --seed "$seed" \
  --sites all \
  --input "$here/inputs" \
  --out "$out" \
  --timeout 300 \
  --force-original

echo "Results: $out"
