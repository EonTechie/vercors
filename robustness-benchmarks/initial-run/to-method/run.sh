#!/usr/bin/env bash
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../../.." && pwd)"
vc="${VC:-$repo/out/vercors/main/runScript.dest/vercors}"
parallel="${PARALLEL:-4}"
run_id="${1:-$(date +%Y%m%d-%H%M%S)}"
out="$here/runs/$run_id"
worklist="$out/worklist.tsv"

test -x "$vc" || { echo "VerCors executable not found: $vc" >&2; exit 1; }
mkdir -p "$out/original-verification"
: > "$worklist"

tail -n +2 "$here/SOURCES.tsv" |
while IFS=$'\t' read -r program copy source sites; do
  site=0
  while test "$site" -lt "$sites"; do
    printf '%s\t%s\t%s\n' "$program" "$site" "$here/$copy" >> "$worklist"
    site=$((site + 1))
  done
done

verify_original() {
  program="$1"
  input="$2"
  log="$out/original-verification/$program.log"
  work="$out/original-verification/$program.work"
  mkdir -p "$work"
  (cd "$work" && "$vc" "$input" > "$log" 2>&1) || true
  rm -rf "$work"
}
export -f verify_original
export out vc

cut -f1,3 "$worklist" | sort -u |
  xargs -P "$parallel" -n 2 bash -c 'verify_original "$@"' _

run_one() {
  program="$1"
  site="$2"
  input="$3"
  dest="$out/results/$program/site-$(printf '%03d' "$site")"
  work="$dest/work"
  mkdir -p "$work"

  if ! grep -q "Verification completed successfully" "$out/original-verification/$program.log"; then
    printf 'ORIGINAL_FAIL\n' > "$dest/result.txt"
    rm -rf "$work"
    return
  fi

  (
    cd "$work"
    ROBUSTNESS_TRANSFORM=to-method TO_METHOD_SITE="$site" \
      "$vc" --robustness "$input" > "$dest/transform.log" 2>&1 || true
    if test -f robustness-transformed.c; then
      cp robustness-transformed.c "$dest/transformed.c"
      "$vc" "$dest/transformed.c" > "$dest/verify.log" 2>&1 || true
    fi
  )
  rm -rf "$work"

  if grep -q "Verification completed successfully" "$dest/verify.log" 2>/dev/null; then
    result=ROBUST
  elif test ! -f "$dest/transformed.c"; then
    result=TRANSFORM_FAIL
  elif grep -q "VerCors has crashed" "$dest/verify.log" 2>/dev/null; then
    result=VERIFY_CRASH
  elif grep -Eq "Parsing failed|Resolution failed|Type checking failed" "$dest/verify.log" 2>/dev/null; then
    result=VERIFY_FRONTEND_FAIL
  else
    result=NOT_ROBUST
  fi
  printf '%s\n' "$result" > "$dest/result.txt"
}
export -f run_one
export out vc

xargs -P "$parallel" -n 3 bash -c 'run_one "$@"' _ < "$worklist"

printf 'program\tsite\tresult\n' > "$out/results.tsv"
while IFS=$'\t' read -r program site input; do
  dest="$out/results/$program/site-$(printf '%03d' "$site")"
  printf '%s\t%s\t%s\n' "$program" "$site" "$(cat "$dest/result.txt")" >> "$out/results.tsv"
done < "$worklist"

echo "Results: $out/results.tsv"
awk -F '\t' 'NR > 1 { count[$3]++ } END { for (key in count) print key, count[key] }' "$out/results.tsv" | sort
