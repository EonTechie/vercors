# Category 2 — Pattern observations (for Claude presentation prompt)

Purpose: notes from the researcher’s observations so Claude can build a presentation about:
- which semantic-transform patterns fit VerCors annotations,
- how annotations were adapted after `semtransforms`,
- what looks automatable vs hard.

Write the talk from **these observations**. Do not invent extra patterns.

Corpus context: transforms run via `semtransforms`; VerCors annotations added/adapted afterward. Seven transform kinds were studied; each section below is filled when observed.

---

## How transforms work (shared)

1. Source is parsed into an **AST**.
2. Candidate **nodes / sites** are scanned.
3. For each transform, predicates decide whether that site is allowed.
4. One chosen node (or insert slot) is rewritten.
5. VerCors annotations are restored or adapted on the result (pycparser drops comment annotations).

---

## Pattern 1 — `add_if1`

### What was observed

- Take a statement node from the AST.
- The check is simple: **it must not be a declaration (`Decl`)**. That is enough.
- The node is wrapped as `if (1) { … } else { … }`.
- **No semantic difference** — same meaning; can be thought of as reversible to the original shape.
- For VerCors, `//@ assert false` can be placed in the **else** branch (dead under `if (1)`).
- On the programs studied for this wrap (including the larger handwritten set, ~67 programs), **no second / different annotation pattern** appeared: same recipe everywhere — keep the original contracts; assert-false only in that dead else.

### Example shape (after transform + annotation)

```c
/*@
  requires a != -2147483648;
  ensures \result == (a >= 0 ? a : -a);
@*/
int absolute(int a)
{
  if (1)
  {
    if (a >= 0) { return a; }
    else { return -a; }
  }
  else
  {
    //@ assert false;
  }
}
```

### Automation note (researcher)

Identity wrap → contracts copy across; only systematic extra is dead-branch `//@ assert false`.

---

## Pattern 2 — `deepen_while_only`

### When it can run (observed checks)

On the AST site:

1. Is there a **`while`**? If not → cannot.
2. Does the condition have **side effects**? If yes → cannot.
3. Is there a **`break`** in the body? If yes → cannot.
4. Is there a **function call** in the condition? For the simple case studied → should **not** be present; then deepen can be done.

### What the transform produced (default)

`semtransforms` also inserts the nondet extern. Example:

```c
extern int __VERIFIER_nondet_int();
int mult_add(int k, int m)
{
  int sum = 0;
  int remaining = m;
  while (0 < remaining)
    while (__VERIFIER_nondet_int() & (0 < remaining))
  {
    sum = sum + k;
    remaining--;
  }
  return sum;
}
```

### VerCors problem and what was changed

- Default used `extern int __VERIFIER_nondet_int()` and bitwise **`&`** inside the inner `while`.
- That **boolean / bitwise mix** caused problems in VerCors.
- Fix used for VerCors: switch to **`_Bool` / `__VERIFIER_nondet_boolean()`** and logical **`&&`** (instead of `int` + `&`).
- The transformer already places that extra extern function; if we keep that shape and only modify type (`int` ↔ boolean) and operator (`&` ↔ `&&`), we can also put a small contract on the nondet declaration.

### Annotation observation (automatable generalization)

- Original **loop invariants can be copied the same way onto both the outer and the inner** loop.
- That copy pattern looks generalizable for automation.
- On the nondet extern, a minimal contract was enough in the example, e.g. `requires true;` — full `ensures` on nondet was not the focus.

### Example shape (VerCors-oriented)

```c
/*@
  requires true;
@*/
extern _Bool __VERIFIER_nondet_boolean();

/*@
  requires k >= 0 && m >= 0;
  ensures \result == k * m;
@*/
int mult_add(int k, int m)
{
  int sum = 0;
  int remaining = m;

  /*@
    loop_invariant 0 <= remaining && remaining <= m;
    loop_invariant sum == (m - remaining) * k;
  @*/
  while (0 < remaining)
  {
    /*@
      loop_invariant 0 <= remaining && remaining <= m;
      loop_invariant sum == (m - remaining) * k;
      decreases remaining;
    @*/
    while (__VERIFIER_nondet_boolean() && (0 < remaining))
    {
      sum = sum + k;
      remaining--;
    }
  }
  return sum;
}
```

---

## Pattern 3 — `if0error`

*(To be filled from researcher observations — next.)*

---

## Pattern 4 — `for2while`

*(To be filled from researcher observations — next.)*

---

## Pattern 5 — `to_method`

*(To be filled from researcher observations — next.)*

---

## Pattern 6 — `insert_method`

*(To be filled from researcher observations — next.)*

---

## Pattern 7 — `to_recursive`

*(To be filled from researcher observations — next.)*

---

## For Claude (when building the presentation)

- Treat each filled pattern section as primary source.
- Emphasize: transform checks → code change → VerCors annotation change → automation feasibility.
- Do not pad with unverified site statistics or rankings unless the researcher adds them.
- Remaining empty patterns: omit from slides or mark “not yet documented.”
