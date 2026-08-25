from pathlib import Path
import re

dirp = Path(__file__).resolve().parent

CONTRACTS = {
    "1_absolute": """/*@
  requires a != -2147483648;
  ensures \\result == (a >= 0 ? a : -a);
@*/
""",
    "2_swap": """/*@
  requires pointer1 != NULL;
  requires pointer2 != NULL;

  context pointer1 != pointer2 ==>
      Perm(pointer1->value, write) **
      Perm(pointer2->value, write);

  context pointer1 == pointer2 ==>
      Perm(pointer1->value, write);

  ensures pointer1->value == \\old(pointer2->value);
  ensures pointer2->value == \\old(pointer1->value);
@*/
""",
    "3_mult_add": """/*@
  requires k >= 0 && m >= 0;
  ensures \\result == k * m;
  decreases k;
@*/
""",
    "3_while_multip": """/*@
  requires k >= 0 && m >= 0;
  ensures \\result == k * m;
@*/
""",
    "4_big": """/*@
  requires length > 0;

  context_everywhere \\pointer(arr, length, read);

  ensures
      (\\forall int x;
          0 <= x && x < length;
          \\result >= arr[x]);

  ensures
      (\\exists int x;
          0 <= x && x < length;
          \\result == arr[x]);
@*/
""",
    "5_bubbleSort": """/*@
  requires length >= 0;

  context_everywhere arr != NULL;
  context_everywhere \\pointer(arr, length, write);

  ensures
    (\\forall int i;
      0 <= i && i <= length - 2;
      arr[i] <= arr[i + 1]);
@*/
""",
    "6_pure": """/*@
  requires 0 <= n && n <= |xs|;
  decreases n;

  pure int prefixSum(seq<int> xs, int n) =
      n == 0
          ? 0
          : prefixSum(xs, n - 1) + xs[n - 1];
@*/

/*@
  given seq<int> xs;

  requires length >= 0;

  context_everywhere list != NULL;
  context_everywhere \\pointer(list, length, read);
  context_everywhere |xs| == length;

  context_everywhere
      (\\forall int x;
          0 <= x && x < length;
          xs[x] == list[x]);

  ensures \\result == prefixSum(xs, length);
@*/
""",
}

WHILE_INV = """  //@ loop_invariant 0 <= remaining && remaining <= m;
  //@ loop_invariant sum == (m - remaining) * k;
  //@ decreases remaining;
"""
FOR_BIG_INV = """  /*@
    loop_invariant 1 <= i && i <= length;
    loop_invariant
        (\\forall int x; 0 <= x && x < i; biggest >= arr[x]);
    loop_invariant
        (\\exists int x; 0 <= x && x < i; biggest == arr[x]);
  @*/
"""
OUTER_BUBBLE = """  //@ loop_invariant 0 <= i && i <= n;
  //@ loop_invariant (\\forall int k; n - i <= k && k < n - 1; arr[k] <= arr[k + 1]);
  //@ loop_invariant (\\forall int g, int h; 0 <= g && g < n - i && n - i <= h && h < n; arr[g] <= arr[h]);
"""
INNER_BUBBLE = """    //@ loop_invariant 0 <= j && j <= n - 1 - i;
    //@ loop_invariant (\\forall int k; n - i <= k && k < n - 1; arr[k] <= arr[k + 1]);
    //@ loop_invariant (\\forall int g, int h; 0 <= g && g < n - i && n - i <= h && h < n; arr[g] <= arr[h]);
    //@ loop_invariant (\\forall int k; 0 <= k && k < j; arr[k] <= arr[j]);
"""
PURE_INV = """  /*@
    loop_invariant 0 <= i && i <= length;
    loop_invariant sum == prefixSum(xs, i);
    decreases length - i;
  @*/
"""


def clean(text: str) -> str:
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = re.sub(
        r"(?m)^\s*extern\s+void\s+__assert_fail\s*\([^;]*\)\s*;\s*\n?",
        "",
        text,
    )
    text = text.replace(
        '__assert_fail("0", "", 3, "reach_error");',
        "//@ assert false;",
    )
    return text


def insert_before_first(code: str, needle: str, block: str) -> str:
    idx = code.find(needle)
    if idx < 0:
        return code
    return code[:idx] + block + code[idx:]


def annotate(stem: str, body: str) -> str:
    body = clean(body)
    if stem == "3_while_multip":
        body = insert_before_first(body, "while (0 < remaining)", WHILE_INV)
    elif stem == "4_big":
        body = insert_before_first(body, "for (i = 1; i < length; i++)", FOR_BIG_INV)
    elif stem == "5_bubbleSort":
        body = insert_before_first(body, "for (int i = 0;", OUTER_BUBBLE)
        body = insert_before_first(body, "for (int j = 0;", INNER_BUBBLE)
    elif stem == "6_pure":
        body = insert_before_first(body, "for (; i < length; i++)", PURE_INV)

    contract = CONTRACTS[stem]
    if stem == "2_swap" and "static void swap" in body:
        return body.replace("static void swap", contract + "static void swap", 1)
    return contract + body


def main():
    sites = sorted(
        p for p in dirp.glob("*_site*.c") if re.search(r"_site\d+\.c$", p.name)
    )
    print("sites", len(sites))
    for p in sites:
        m = re.match(r"(.+)_site(\d+)\.c$", p.name)
        stem, idx = m.group(1), m.group(2)
        raw = p.read_text(encoding="utf-8")
        cleaned = clean(raw)
        p.write_text(cleaned, encoding="utf-8", newline="\n")
        annotated = annotate(stem, cleaned)
        dest = dirp / f"{stem}_site{idx}_transformed_added_annotations.c"
        dest.write_text(annotated, encoding="utf-8", newline="\n")
        left_fail = "__assert_fail" in cleaned
        print(f"{p.name}: cleaned assert_fail_left={left_fail}")

    sample = (dirp / "1_absolute_site1.c").read_text(encoding="utf-8")
    print("--- sample ---")
    print(sample)


if __name__ == "__main__":
    main()
