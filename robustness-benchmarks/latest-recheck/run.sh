#!/usr/bin/env bash
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
driver="$repo/util/robustness/run-benchmark.sh"
wrapper="$here/vercors-with-timeout.sh"
config="$here/config.env"
real_vercors="${VC:-$repo/out/vercors/main/runScript.dest/vercors}"
run_id="${1:-current-$(date +%Y%m%d-%H%M%S)}"
out="$here/runs/$run_id"
established_out="$out/earlier-four-transformations"
to_method_out="$out/to-method"
started="$(date -Iseconds)"

[[ -x "$real_vercors" ]] || {
  echo "VerCors executable not found: $real_vercors" >&2
  exit 1
}
[[ ! -e "$out" ]] || {
  echo "Output already exists: $out" >&2
  exit 1
}

mkdir -p "$out"
exec 9>"$here/runs/.validation.lock"
flock -n 9 || {
  echo "Another current-code validation is already running." >&2
  exit 1
}

export REAL_VERCORS="$real_vercors"
export VERCORS_CALL_TIMEOUT_SECONDS="${VERCORS_CALL_TIMEOUT_SECONDS:-3900}"

cat > "$out/validation.env" <<EOF
STARTED=$started
RUN_ID=$run_id
HEAD=$(git -C "$repo" rev-parse HEAD)
VERCORS=$real_vercors
VERCORS_INTERNAL_TIMEOUT_SECONDS=3600
VERCORS_PROCESS_TIMEOUT_SECONDS=$VERCORS_CALL_TIMEOUT_SECONDS
OVERALL_LIMIT_IS_SET_BY_LAUNCH_COMMAND=72h
ESTABLISHED_INPUTS=$repo/robustness-benchmarks/initial-run/earlier-four-transformations/inputs
TO_METHOD_INPUTS=$repo/robustness-benchmarks/initial-run/to-method/inputs
EOF

git -C "$repo" status --short > "$out/working-tree-status.txt"
git -C "$repo" diff --binary HEAD -- src util build.sc > "$out/source-state.diff"
sha256sum "$real_vercors" > "$out/vercors-before.sha256"

finish() {
  status=$?
  trap - EXIT
  {
    echo "FINISHED=$(date -Iseconds)"
    echo "EXIT_STATUS=$status"
  } >> "$out/validation.env"
  sha256sum "$real_vercors" > "$out/vercors-after.sha256" 2>/dev/null || true
  if cmp -s "$out/vercors-before.sha256" "$out/vercors-after.sha256"; then
    echo "VERCORS_EXECUTABLE_UNCHANGED=yes" >> "$out/validation.env"
  else
    echo "VERCORS_EXECUTABLE_UNCHANGED=no" >> "$out/validation.env"
  fi
  exit "$status"
}
trap finish EXIT

run_transform_set() {
  transform="$1"
  input="$2"
  destination="$3"
  echo "[$(date -Iseconds)] starting $transform"
  "$driver" \
    --config "$config" \
    --mode preferred \
    --transform "$transform" \
    --repeat 1 \
    --sites all \
    --input "$input" \
    --out "$destination" \
    --vercors "$wrapper" \
    --timeout 3600
  echo "[$(date -Iseconds)] finished $transform"
}

established_inputs="$repo/robustness-benchmarks/initial-run/earlier-four-transformations/inputs"
to_method_inputs="$repo/robustness-benchmarks/initial-run/to-method/inputs"

for transform in add-if-zero add-if-one for-to-while deepen-while; do
  run_transform_set "$transform" "$established_inputs" "$established_out"
done

run_transform_set to-method "$to_method_inputs" "$to_method_out"

python3 "$here/summarize.py" "$out"

echo "Results: $out"
cat "$out/summary.tsv"
