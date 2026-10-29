#!/usr/bin/env bash
# Driver, not VerCors itself:
#   1) ordinary vercors on each original .c
#   2) vercors --robustness (dump only, no Silicon)
#   3) ordinary vercors on each mutant .c
# Edit util/robustness/config.env, then run this script with no arguments.
# Values in this file are fallbacks if a key is missing from the config.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DUMP_NAME="robustness-transformed.c"
DEFAULT_VCT="$ROOT/out/vercors/main/runScript.dest/vercors"
DEFAULT_INPUT="$ROOT/robustness-benchmarks/pilot/inputs"
DEFAULT_OUT="$ROOT/robustness-results/post-resolution"
ALL_SITE_VARS=(
  ADD_IF_ZERO_SITE ADD_IF_ONE_SITE FOR_TO_WHILE_SITE
  DEEPEN_WHILE_SITE TO_METHOD_SITE
)

MODE="preferred"
TRANSFORM="add-if-zero"
TRANSFORM_SET=0
REPEAT=1
SEED=""
SITES="all"
INPUT="$DEFAULT_INPUT"
OUT="$DEFAULT_OUT"
VCT=""
PROGRAMS=""
TIMEOUT=""
VERIFY_MUTANTS=1
VERIFY_ORIGINALS=1
STAMP=0
DRY_RUN=0
FORCE_ORIGINAL=0
CONFIG_FILE="$SCRIPT_DIR/config.env"
STARTED=""
FINISHED=""
RUN_ID=""
RUN_LOG=""
RUN_DESC=""
RUN_FILES=""
OUT_REQUESTED=""

die() { echo "ERROR: $*" >&2; exit 1; }
log() {
  local line="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
  echo "$line"
  if [[ -n "$RUN_LOG" ]]; then
    echo "$line" >> "$RUN_LOG"
  fi
}

resolve_path() {
  local p="$1"
  if [[ -z "$p" ]]; then
    echo "$p"
  elif [[ "$p" == /* ]]; then
    echo "$p"
  else
    echo "$ROOT/$p"
  fi
}

load_config() {
  local file="$1" line key value
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%%#*}"
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "$line" != *=* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    value="${value#\"}"
    value="${value%\"}"
    value="${value#\'}"
    value="${value%\'}"
    case "$key" in
      MODE) MODE="$value" ;;
      TRANSFORM) TRANSFORM="$value"; TRANSFORM_SET=1 ;;
      REPEAT) REPEAT="$value" ;;
      SEED) SEED="$value" ;;
      SITES) SITES="$value" ;;
      INPUT) INPUT="$value" ;;
      OUT) OUT="$value" ;;
      PROGRAMS) PROGRAMS="$value" ;;
      VCT) VCT="$value" ;;
      TIMEOUT) TIMEOUT="$value" ;;
      VERIFY_MUTANTS) VERIFY_MUTANTS="$value" ;;
      VERIFY_ORIGINALS) VERIFY_ORIGINALS="$value" ;;
      FORCE_ORIGINAL) FORCE_ORIGINAL="$value" ;;
      STAMP) STAMP="$value" ;;
      DRY_RUN) DRY_RUN="$value" ;;
    esac
  done < "$file"
}

usage() {
  cat <<'EOF'
Usage:  ./util/robustness/run-benchmark.sh

Edit util/robustness/config.env, then run the command above.
A flag on the command line overrides the matching config value.

--config PATH
--mode preferred|multiple|mixed|chain|verify-only
--transform add-if-zero|add-if-one|for-to-while|deepen-while|to-method
--repeat N   --seed N   --sites all|0,2,5
--input PATH  --out PATH  --programs 1_absolute,2_swap
--vercors PATH  --timeout SEC
--skip-verify  --skip-original  --force-original  --stamp  --dry-run
EOF
}

site_var_for() {
  case "$1" in
    add-if-zero|add_if_zero) echo ADD_IF_ZERO_SITE ;;
    add-if-one|add_if_one) echo ADD_IF_ONE_SITE ;;
    for-to-while|for_to_while) echo FOR_TO_WHILE_SITE ;;
    deepen-while|deepen_while) echo DEEPEN_WHILE_SITE ;;
    to-method|to_method) echo TO_METHOD_SITE ;;
    *) echo "" ;;
  esac
}

clear_robustness_env() {
  local v
  for v in "${ALL_SITE_VARS[@]}" ROBUSTNESS_TRANSFORM ROBUSTNESS_SPIN ROBUSTNESS_SEED; do
    unset "$v"
  done
}

# Full run descriptor. Always written under OUT/logs/run-YYYYMMDD-HHMMSS.env
# even when STAMP=0 (output folder has no date in its name).
write_run_env() {
  local dest="$1" names="" f
  for f in "${FILES[@]}"; do
    [[ -n "$names" ]] && names+=","
    names+="$(basename "$f" .c)"
  done
  cat > "$dest" <<EOF
STARTED=$STARTED
FINISHED=${FINISHED:-}
RUN_ID=$RUN_ID
CONFIG_FILE=$(resolve_path "$CONFIG_FILE")
MODE=$MODE
TRANSFORM=$TRANSFORM
REPEAT=$REPEAT
SEED=$SEED
SITES=$SITES
INPUT=$INPUT
OUT_REQUESTED=$OUT_REQUESTED
OUT=$OUT
PROGRAMS=$PROGRAMS
FILE_COUNT=${#FILES[@]}
FILES=$names
VCT=$VCT
TIMEOUT=$TIMEOUT
VERIFY_MUTANTS=$VERIFY_MUTANTS
VERIFY_ORIGINALS=$VERIFY_ORIGINALS
FORCE_ORIGINAL=$FORCE_ORIGINAL
STAMP=$STAMP
DRY_RUN=$DRY_RUN
EOF
  if [[ -n "$RUN_DESC" && "$dest" != "$RUN_DESC" ]]; then
    cp -f "$dest" "$RUN_DESC"
  fi
  if [[ -n "$RUN_FILES" ]]; then
    printf '%s\n' "${FILES[@]}" > "$RUN_FILES"
  fi
}

finalize_run() {
  local ec=$?
  if [[ -z "$STARTED" || -z "${OUT:-}" || ! -d "${OUT:-}" ]]; then
    return
  fi
  FINISHED="$(date -Iseconds)"
  write_run_env "$OUT/run.env" || true
  if [[ "$ec" -eq 0 ]]; then
    log "done  $OUT" || true
  else
    log "failed  $OUT  exit=$ec" || true
  fi
}

verify_status() {
  local logf="$1"
  if [[ "$DRY_RUN" == "1" ]]; then echo DRY; return; fi
  if [[ -f "$logf" ]] && grep -q "Verification completed successfully" "$logf"; then
    echo PASS
  else
    echo FAIL
  fi
}

transform_status() {
  local logf="$1"
  if [[ "$DRY_RUN" == "1" ]]; then echo DRY; return; fi
  if [[ -f "$logf" ]] && grep -q "Robustness transformation completed" "$logf"; then
    echo PASS
  else
    echo FAIL
  fi
}

candidate_count() {
  local n
  n="$(grep -E 'candidate count = [0-9]+' "$1" 2>/dev/null | head -n 1 | grep -oE '[0-9]+$' || true)"
  echo "${n:-0}"
}

spin_trace() {
  grep '\[RobustnessSpin\] round' "$1" 2>/dev/null \
    | grep -v 'has 0 candidates' | grep -v 'stopping' \
    | sed -E 's/.*round [0-9]+: ([a-z0-9-]+: [0-9]+) \/.*/\1/' \
    | paste -sd '; ' - || true
}

verdict() {
  local original="$1" tstatus="$2" vstatus="$3" ncand="$4" vlog="${5:-}"
  if [[ "$ncand" == "0" ]]; then echo NO_SITE
  elif [[ "$tstatus" != "PASS" ]]; then echo TRANSFORM_FAIL
  elif [[ "$original" != "PASS" ]]; then echo SKIP
  elif [[ "$vstatus" == "PASS" ]]; then echo ROBUST
  elif [[ -n "$vlog" ]] && grep -q "VerCors has crashed" "$vlog" 2>/dev/null; then echo VERIFY_CRASH
  elif [[ -n "$vlog" ]] && grep -q "VERCORS_PROCESS_TIMEOUT" "$vlog" 2>/dev/null; then echo VERIFY_TIMEOUT
  elif [[ -n "$vlog" ]] && grep -q "Parsing failed" "$vlog" 2>/dev/null; then echo VERIFY_PARSE_FAIL
  elif [[ -n "$vlog" ]] && grep -Eq "Resolution failed|Type checking failed|typecheck" "$vlog" 2>/dev/null; then echo VERIFY_FRONTEND_FAIL
  else echo NOT_ROBUST
  fi
}

collect_files() {
  local path="$1" f base
  FILES=()
  if [[ -f "$path" ]]; then
    FILES+=("$path")
  elif [[ -d "$path" ]]; then
    shopt -s nullglob
    for f in "$path"/*.c; do
      base="$(basename "$f" .c)"
      if [[ -n "$PROGRAMS" ]]; then
        case ",$PROGRAMS," in
          *,"$base",*) ;;
          *) continue ;;
        esac
      fi
      FILES+=("$f")
    done
    shopt -u nullglob
  else
    die "input not found: $path"
  fi
  [[ ${#FILES[@]} -gt 0 ]] || die "no .c files in: $path"
}

find_vct() {
  if [[ -n "$VCT" ]]; then
    [[ -x "$VCT" ]] || die "--vercors is not executable: $VCT"
    return
  fi
  if [[ -n "${VERCORS:-}" && -x "${VERCORS}" ]]; then VCT="$VERCORS"; return; fi
  if [[ -x "$DEFAULT_VCT" ]]; then VCT="$DEFAULT_VCT"; return; fi
  if [[ -x "$ROOT/bin/vct" ]]; then VCT="$ROOT/bin/vct"; return; fi
  die "vercors not found. Run ./mill -j 0 vercors.main.runScript first."
}

run_verify() {
  local src="$1" logf="$2" extra=()
  if [[ -n "$TIMEOUT" ]]; then extra+=(--dev-total-timeout "$TIMEOUT"); fi
  if [[ "$DRY_RUN" == "1" ]]; then
    echo "DRY  $VCT ${extra[*]} $src"
    return 0
  fi
  local verify_work
  verify_work="$(mktemp -d)"
  clear_robustness_env
  set +e
  (cd "$verify_work" && "$VCT" "${extra[@]}" "$src") > "$logf" 2>&1
  set -e
  rm -rf "$verify_work"
}

run_transform() {
  local src="$1" logf="$2"
  shift 2
  if [[ "$DRY_RUN" == "1" ]]; then
    echo "DRY  $VCT --robustness $* $src"
    return 0
  fi
  local work
  work="$(mktemp -d)"
  (
    cd "$work"
    set +e
    "$VCT" --robustness "$@" "$src" > "$logf" 2>&1
    set -e
  )
  LAST_WORK="$work"
  LAST_DUMP="$work/$DUMP_NAME"
}

copy_dump() {
  local dest="$1"
  if [[ "$DRY_RUN" == "1" ]]; then echo "DRY  cp dump -> $dest"; return 0; fi
  if [[ -f "${LAST_DUMP:-}" ]]; then cp -f "$LAST_DUMP" "$dest"; fi
  if [[ -n "${LAST_WORK:-}" && -d "$LAST_WORK" ]]; then rm -rf "$LAST_WORK"; fi
  LAST_WORK=""
  LAST_DUMP=""
}

_scan=("$@")
for ((i = 0; i < ${#_scan[@]}; i++)); do
  if [[ "${_scan[i]}" == "--config" ]]; then
    CONFIG_FILE="${_scan[i+1]:-}"
  fi
done
load_config "$CONFIG_FILE"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --config) CONFIG_FILE="${2:-}"; shift 2 ;;
    --mode) MODE="${2:-}"; shift 2 ;;
    --transform) TRANSFORM="${2:-}"; TRANSFORM_SET=1; shift 2 ;;
    --repeat) REPEAT="${2:-}"; shift 2 ;;
    --seed) SEED="${2:-}"; shift 2 ;;
    --sites) SITES="${2:-}"; shift 2 ;;
    --input) INPUT="${2:-}"; shift 2 ;;
    --out) OUT="${2:-}"; shift 2 ;;
    --vercors) VCT="${2:-}"; shift 2 ;;
    --programs) PROGRAMS="${2:-}"; shift 2 ;;
    --timeout) TIMEOUT="${2:-}"; shift 2 ;;
    --skip-verify) VERIFY_MUTANTS=0; shift ;;
    --skip-original) VERIFY_ORIGINALS=0; shift ;;
    --force-original) FORCE_ORIGINAL=1; shift ;;
    --stamp) STAMP=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    --help|-h) usage; exit 0 ;;
    *) die "unknown flag: $1" ;;
  esac
done

case "$MODE" in
  preferred|multiple|mixed|chain|verify-only) ;;
  *) die "--mode must be one of: preferred multiple mixed chain verify-only" ;;
esac
[[ "$REPEAT" =~ ^[0-9]+$ ]] && [[ "$REPEAT" -ge 1 ]] || die "--repeat must be a positive integer"

PROGRAMS="${PROGRAMS// /}"
TRANSFORM="${TRANSFORM// /}"
INPUT="$(resolve_path "$INPUT")"
OUT="$(resolve_path "$OUT")"
OUT_REQUESTED="$OUT"
if [[ -n "$VCT" ]]; then VCT="$(resolve_path "$VCT")"; fi
RUN_ID="$(date +%Y%m%d-%H%M%S)"
STARTED="$(date -Iseconds)"
if [[ "$STAMP" == "1" ]]; then
  OUT="$OUT/run-$RUN_ID"
fi

find_vct
collect_files "$INPUT"
mkdir -p "$OUT/logs"
if [[ -e "$OUT/logs/run-$RUN_ID.env" ]]; then
  RUN_ID="${RUN_ID}-$$"
fi
RUN_LOG="$OUT/logs/run-$RUN_ID.log"
RUN_DESC="$OUT/logs/run-$RUN_ID.env"
RUN_FILES="$OUT/logs/run-$RUN_ID.files"
trap finalize_run EXIT
write_run_env "$OUT/run.env"
if [[ -f "$CONFIG_FILE" ]]; then
  cp -f "$CONFIG_FILE" "$OUT/config.env"
fi
{
  echo "---- run descriptor ----"
  cat "$OUT/run.env"
  echo "---- files ----"
  cat "$RUN_FILES"
  echo "---- log ----"
} >> "$RUN_LOG"

log "mode=$MODE  transform=$TRANSFORM  files=${#FILES[@]}  out=$OUT"

ORIG_DIR="$OUT/_original-verify"
mkdir -p "$ORIG_DIR"
declare -A ORIG_STATUS

verify_originals() {
  local src base dest st
  log "original verify"
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$ORIG_DIR/${base}.verify.log"
    if [[ "$VERIFY_ORIGINALS" != "1" ]]; then
      ORIG_STATUS["$base"]="SKIPPED"
      continue
    fi
    if [[ "$FORCE_ORIGINAL" != "1" && -f "$dest" ]]; then
      ORIG_STATUS["$base"]="$(verify_status "$dest")"
      log "  reuse $base  original=${ORIG_STATUS[$base]}"
      continue
    fi
    log "  verify $base"
    run_verify "$src" "$dest"
    ORIG_STATUS["$base"]="$(verify_status "$dest")"
    log "  ${ORIG_STATUS[$base]}  $base"
  done
}

if [[ "$MODE" == "verify-only" ]]; then
  verify_originals
  echo "program	original"
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    echo "$base	${ORIG_STATUS[$base]}"
  done | tee "$OUT/summary.tsv"
  exit 0
fi

verify_originals

run_preferred() {
  local src base dest n i site_list sv tlog vlog tst vst vd tpass tfail vpass vfail disc_dump
  local first_tf="${TRANSFORM%%,*}"
  [[ "$TRANSFORM" == "$first_tf" ]] || die "preferred takes one transform; use --mode chain for a list"
  sv="$(site_var_for "$first_tf")"
  [[ -n "$sv" ]] || die "unknown transform: $TRANSFORM"

  local tdir="$OUT/$first_tf" summary verdicts
  mkdir -p "$tdir"
  summary="$tdir/summary.tsv"
  verdicts="$tdir/verdict.tsv"
  echo "program	original	candidates	transform_pass	transform_fail	verify_pass	verify_fail" > "$summary"
  echo "program	site	original	transform	verify	verdict" > "$verdicts"

  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    if [[ -f "$ORIG_DIR/${base}.verify.log" ]]; then
      cp -f "$ORIG_DIR/${base}.verify.log" "$dest/original.verify.log"
    fi
    tpass=0; tfail=0; vpass=0; vfail=0

    log "  $base  discovery"
    clear_robustness_env
    export ROBUSTNESS_TRANSFORM="$first_tf"
    export "$sv=0"
    run_transform "$src" "$dest/discovery.log"
    n="$(candidate_count "$dest/discovery.log")"
    disc_dump=""
    if [[ -f "${LAST_DUMP:-}" ]]; then
      disc_dump="$(mktemp)"
      cp -f "$LAST_DUMP" "$disc_dump"
    fi
    if [[ -n "${LAST_WORK:-}" && -d "$LAST_WORK" ]]; then rm -rf "$LAST_WORK"; fi
    LAST_WORK=""; LAST_DUMP=""

    site_list=()
    if [[ "$SITES" == "all" ]]; then
      if [[ "$n" -gt 0 ]]; then
        for ((i = 0; i < n; i++)); do site_list+=("$i"); done
      fi
    else
      IFS=',' read -r -a site_list <<< "$SITES"
    fi

    if [[ ${#site_list[@]} -eq 0 ]]; then
      echo "$base	${ORIG_STATUS[$base]}	$n	0	0	0	0" >> "$summary"
      echo "$base	-	${ORIG_STATUS[$base]}	SKIP	SKIP	NO_SITE" >> "$verdicts"
      [[ -n "$disc_dump" ]] && rm -f "$disc_dump"
      continue
    fi

    for i in "${site_list[@]}"; do
      tlog="$dest/site-${i}.transform.log"
      vlog="$dest/site-${i}.verify.log"
      if [[ "$n" -gt 0 && "$i" -ge "$n" ]]; then
        echo "$base	$i	${ORIG_STATUS[$base]}	FAIL	SKIP	TRANSFORM_FAIL" >> "$verdicts"
        tfail=$((tfail + 1))
        continue
      fi
      if [[ "$i" == "0" && -f "$dest/discovery.log" ]]; then
        cp -f "$dest/discovery.log" "$tlog"
        if [[ -n "$disc_dump" && -f "$disc_dump" ]]; then
          cp -f "$disc_dump" "$dest/site-0.c"
        fi
      else
        log "  $base  transform site=$i"
        clear_robustness_env
        export ROBUSTNESS_TRANSFORM="$first_tf"
        export "$sv=$i"
        run_transform "$src" "$tlog"
        copy_dump "$dest/site-${i}.c"
      fi
      tst="$(transform_status "$tlog")"
      if [[ "$tst" == "PASS" ]]; then tpass=$((tpass + 1)); else tfail=$((tfail + 1)); fi
      vst="SKIP"
      if [[ "$VERIFY_MUTANTS" != "1" ]]; then
        vst="SKIPPED"
      elif [[ "$tst" != "PASS" || ! -f "$dest/site-${i}.c" ]]; then
        vst="FAIL"; vfail=$((vfail + 1))
      else
        log "  $base  verify site=$i"
        run_verify "$dest/site-${i}.c" "$vlog"
        vst="$(verify_status "$vlog")"
        if [[ "$vst" == "PASS" ]]; then vpass=$((vpass + 1)); else vfail=$((vfail + 1)); fi
      fi
      vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "$n" "$vlog")"
      echo "$base	$i	${ORIG_STATUS[$base]}	$tst	$vst	$vd" >> "$verdicts"
      log "  $vd  $base site=$i"
    done
    echo "$base	${ORIG_STATUS[$base]}	$n	$tpass	$tfail	$vpass	$vfail" >> "$summary"
    [[ -n "$disc_dump" ]] && rm -f "$disc_dump"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
  echo
  column -t -s $'\t' "$verdicts" 2>/dev/null || cat "$verdicts"
}

run_multiple() {
  local src base dest tlog vlog n tst vst vd sv first_tf="${TRANSFORM%%,*}"
  [[ "$TRANSFORM" == "$first_tf" ]] || die "multiple takes one transform; use --mode chain for a list"
  sv="$(site_var_for "$first_tf")"
  [[ -n "$sv" ]] || die "unknown transform: $TRANSFORM"
  local tdir="$OUT/multiple/$first_tf/repeat-$REPEAT"
  local summary="$OUT/multiple/summary.tsv"
  mkdir -p "$tdir" "$OUT/multiple"
  if [[ ! -f "$summary" ]]; then
    echo "transform	repeat	program	transform_status	verify	candidates_round1	verdict" > "$summary"
  fi
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    tlog="$dest/transform.log"
    vlog="$dest/verify.log"
    log "  $base  transform x$REPEAT"
    clear_robustness_env
    export ROBUSTNESS_TRANSFORM="$first_tf"
    if [[ "$SITES" != "all" ]]; then export "$sv=$SITES"; fi
    run_transform "$src" "$tlog" --robustness-repeat "$REPEAT"
    copy_dump "$dest/repeat-${REPEAT}.c"
    n="$(candidate_count "$tlog")"
    tst="$(transform_status "$tlog")"
    vst="SKIP"
    if [[ "$VERIFY_MUTANTS" != "1" ]]; then vst="SKIPPED"
    elif [[ "$tst" == "PASS" && -f "$dest/repeat-${REPEAT}.c" ]]; then
      run_verify "$dest/repeat-${REPEAT}.c" "$vlog"
      vst="$(verify_status "$vlog")"
    else vst="FAIL"
    fi
    vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "$n" "$vlog")"
    echo "$first_tf	$REPEAT	$base	$tst	$vst	$n	$vd" >> "$summary"
    log "  $vd  $base"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
}

run_mixed() {
  local src base dest tlog vlog tst vst vd trace extra
  local tdir="$OUT/spin/repeat-$REPEAT"
  local summary="$OUT/spin/summary.tsv"
  mkdir -p "$tdir"
  echo "program	transform_status	verify	seed	repeat	trace	verdict" > "$summary"
  extra=(--robustness-spin --robustness-repeat "$REPEAT")
  if [[ -n "$SEED" ]]; then extra+=(--robustness-seed "$SEED"); fi
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    tlog="$dest/transform.log"
    vlog="$dest/verify.log"
    log "  $base  spin x$REPEAT"
    clear_robustness_env
    if [[ "$TRANSFORM_SET" == "1" && -n "$TRANSFORM" ]]; then
      export ROBUSTNESS_TRANSFORM="$TRANSFORM"
    fi
    run_transform "$src" "$tlog" "${extra[@]}"
    copy_dump "$dest/spin-${REPEAT}.c"
    tst="$(transform_status "$tlog")"
    trace="$(spin_trace "$tlog")"
    vst="SKIP"
    if [[ "$VERIFY_MUTANTS" != "1" ]]; then vst="SKIPPED"
    elif [[ "$tst" == "PASS" && -f "$dest/spin-${REPEAT}.c" ]]; then
      run_verify "$dest/spin-${REPEAT}.c" "$vlog"
      vst="$(verify_status "$vlog")"
    else vst="FAIL"
    fi
    vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "1" "$vlog")"
    echo "$base	$tst	$vst	${SEED:-auto}	$REPEAT	$trace	$vd" >> "$summary"
    log "  $vd  $base"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
}

run_chain() {
  local src base dest tlog vlog tst vst vd n names nnames slug sv
  IFS=',' read -r -a names <<< "$TRANSFORM"
  nnames=${#names[@]}
  [[ "$nnames" -ge 1 ]] || die "chain needs a comma-separated --transform list"
  if [[ "$REPEAT" -eq 1 && "$nnames" -gt 1 ]]; then REPEAT="$nnames"; fi
  slug="$(echo "$TRANSFORM" | tr ',' '_' | tr -s '_')"
  local tdir="$OUT/chain/$slug/repeat-$REPEAT"
  local summary="$OUT/chain/summary.tsv"
  mkdir -p "$tdir" "$OUT/chain"
  if [[ ! -f "$summary" ]]; then
    echo "chain	repeat	program	transform_status	verify	candidates_round1	verdict" > "$summary"
  fi
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    tlog="$dest/transform.log"
    vlog="$dest/verify.log"
    log "  $base  chain x$REPEAT"
    clear_robustness_env
    export ROBUSTNESS_TRANSFORM="$TRANSFORM"
    if [[ "$SITES" != "all" ]]; then
      sv="$(site_var_for "${names[0]}")"
      [[ -n "$sv" ]] && export "$sv=$SITES"
    fi
    run_transform "$src" "$tlog" --robustness-repeat "$REPEAT"
    copy_dump "$dest/chain-${REPEAT}.c"
    n="$(candidate_count "$tlog")"
    tst="$(transform_status "$tlog")"
    vst="SKIP"
    if [[ "$VERIFY_MUTANTS" != "1" ]]; then vst="SKIPPED"
    elif [[ "$tst" == "PASS" && -f "$dest/chain-${REPEAT}.c" ]]; then
      run_verify "$dest/chain-${REPEAT}.c" "$vlog"
      vst="$(verify_status "$vlog")"
    else vst="FAIL"
    fi
    vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "1" "$vlog")"
    echo "$TRANSFORM	$REPEAT	$base	$tst	$vst	$n	$vd" >> "$summary"
    log "  $vd  $base"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
}

case "$MODE" in
  preferred) run_preferred ;;
  multiple)  run_multiple ;;
  mixed)     run_mixed ;;
  chain)     run_chain ;;
esac
