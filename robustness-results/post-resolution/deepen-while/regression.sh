#!/usr/bin/env bash
# Repeat / mixed checks for deepen-while. Transform only, no Silicon.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
VCT="$ROOT/out/vercors/main/runScript.dest/vercors"
EX="$ROOT/examples/concepts/basic/EXAMPLES/3_while_multip.c"
OUT="$(cd "$(dirname "$0")" && pwd)/regression"

unset ADD_IF_ZERO_SITE ADD_IF_ONE_SITE FOR_TO_WHILE_SITE DEEPEN_WHILE_SITE TO_METHOD_SITE ROBUSTNESS_SPIN ROBUSTNESS_SEED

mkdir -p "$OUT"
fail() { echo "FAIL: $*" >&2; exit 1; }

echo "===== 1) DeepenWhile x2 on 3_while_multip ====="
work=$(mktemp -d)
cd "$work"
export ROBUSTNESS_TRANSFORM=deepen-while
export DEEPEN_WHILE_SITE=0
"$VCT" --robustness --robustness-repeat 2 "$EX" > "$OUT/repeat-2.transform.log" 2>&1
cp -f robustness-transformed.c "$OUT/repeat-2.c"

grep -E '\[DeepenWhile\]' "$OUT/repeat-2.transform.log"

pass1=$(grep -c '\[DeepenWhile\] candidate count = 1' "$OUT/repeat-2.transform.log" || true)
[ "$pass1" = 2 ] || fail "expected two DeepenWhile passes with count=1, got $pass1"
grep -q '\[DeepenWhile\] skip generated adapter' "$OUT/repeat-2.transform.log" ||
  fail "second pass did not skip a generated adapter"
grep -q 'generated=false' "$OUT/repeat-2.transform.log" ||
  fail "eligible candidate should be generated=false"
! grep -q 'candidate .*generated=true' "$OUT/repeat-2.transform.log" ||
  fail "generated adapter appeared as a DeepenWhile candidate"

# outer I, no decreases; new adapter I, no decreases; deepest I + decreases
python3 - "$OUT/repeat-2.c" << 'PY' || fail "repeat-2 contract shape"
import pathlib, re, sys
text = pathlib.Path(sys.argv[1]).read_text()
whiles = list(re.finditer(r"/\*@\s*(.*?)\s*@\*/\s*while", text, re.S))
if len(whiles) != 3:
    raise SystemExit(f"expected 3 annotated whiles, got {len(whiles)}")
blocks = [m.group(1) for m in whiles]
def has_dec(b): return "decreases" in b
def has_inv(b): return "loop_invariant" in b
if not (has_inv(blocks[0]) and not has_dec(blocks[0])):
    raise SystemExit("outer should have invariants and no decreases")
if not (has_inv(blocks[1]) and not has_dec(blocks[1])):
    raise SystemExit("new adapter should have invariants and no decreases")
if not (has_inv(blocks[2]) and has_dec(blocks[2])):
    raise SystemExit("deepest adapter should keep decreases")
print("contract shape OK")
PY

echo "===== 2) DeepenWhile -> AddIfZero(adapter) -> DeepenWhile ====="
work=$(mktemp -d)
cd "$work"
export ROBUSTNESS_TRANSFORM=deepen-while,add-if-zero,deepen-while
export DEEPEN_WHILE_SITE=0
export ADD_IF_ZERO_SITE=4
"$VCT" --robustness --robustness-repeat 3 "$EX" > "$OUT/mixed.transform.log" 2>&1
cp -f robustness-transformed.c "$OUT/mixed.c"

grep -E '\[DeepenWhile\]|\[AddIfZero\] candidate |\[AddIfZero\] SELECTED|\[AddIfZero\] WRAP|deepenGenerated' "$OUT/mixed.transform.log"

grep -q 'deepenGenerated=true' "$OUT/mixed.transform.log" ||
  fail "AddIfZero did not see a generated adapter loop"
grep -q '\[AddIfZero\] SELECTED INDEX = 4' "$OUT/mixed.transform.log" ||
  fail "AddIfZero did not wrap site 4 (generated adapter Loop)"
grep -q '\[AddIfZero\] WRAP class=Loop' "$OUT/mixed.transform.log" ||
  fail "AddIfZero did not wrap a Loop"
grep -q 'skip generated adapter' "$OUT/mixed.transform.log" ||
  fail "later DeepenWhile did not skip the generated adapter after AddIfZero"
grep -q 'branch\[1\]' "$OUT/mixed.transform.log" ||
  fail "skipped adapter path should be inside the AddIfZero else arm"
grep -c '\[DeepenWhile\] candidate count = 1' "$OUT/mixed.transform.log" | grep -qx 2 ||
  fail "both DeepenWhile passes should have exactly 1 eligible outer while"

echo "PASS"
