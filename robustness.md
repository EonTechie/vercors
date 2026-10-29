# How to add a new robustness transform

This guide explains how to add a transformation to the existing robustness mode
and benchmark driver. It uses `my-transform` and `MY_TRANSFORM_SITE` as placeholders.

## 1. Pick the two strings

- Transform name: `my-transform`
- Site variable: `MY_TRANSFORM_SITE`

## 2. List the places the rewrite may touch

Add `src/rewrite/vct/rewrite/MyTransformSites.scala` with
`collect(program)` that returns the allowed nodes in a stable order.
Index `0` is the first item.

Use [ToMethodSites.scala](src/rewrite/vct/rewrite/ToMethodSites.scala) or
[CStatementSites.scala](src/rewrite/vct/rewrite/CStatementSites.scala) as a
reference for visiting `CTranslationUnit`, `CFunctionDefinition` and `Procedure`
nodes. Their `blacklistedFunction` checks exclude verification helpers, including
`reach_error`, `abort`, `assume_abort_if_not` and names beginning with `__VERIFIER_`.
Define the new transformation's own eligibility and traversal rules in its
collector; the existing collectors accept different statement shapes.

## 3. Write the rewriter

Add `src/rewrite/vct/rewrite/MyTransform.scala`, following
[AddIfZero.scala](src/rewrite/vct/rewrite/AddIfZero.scala): a `case object`
extends `RewriterBuilder`, and a `case class` extends `Rewriter[Pre]`.
Keeping them in the same file follows the existing transformation structure.

Those steps go in the `case class` in `MyTransform.scala`, in
`dispatch(program)` (see `AddIfZero.scala`):

1. `val candidates = MyTransformSites.collect(program)`
2. `RobustnessSiteSelection.nextIndex("MY_TRANSFORM_SITE", candidates.size)`
   and keep `candidates(index)` when that returns an index. Call
   `nextIndex` even if the list is empty. If there are no candidates, do
   not change the program.
3. `super.dispatch(program)`

When you visit a statement (still in that same `case class`): if it is
the selected site, apply your edit once. If it is any other statement,
leave it unchanged (`stat.rewriteDefault()`). That is how the rest of
the program stays the same.

If you replace a `{ }` body with something that is not a block, call
`RobustnessStatementWrap.keepSurroundingBraces`.

## 4. Register the name

`TRANSFORM=my-transform` is only text in `config.env`. Until
`src/main/vct/main/modes/Robustness.scala` `parseTransform` turns that
text into your `case object`, VerCors does not know which rewrite to
run. In that file do three things, the same way `add-if-zero` is wired:

1. At the top of `Robustness.scala`, in `import vct.col.rewrite.{ ... }`,
   add `MyTransform` next to `AddIfZero` (so the file can name your
   `case object`).
2. In the same file, inside `parseTransform`’s `match`, add a line like
   the add-if-zero one: `case "my-transform" => MyTransform`. That is the
   line that turns the text `my-transform` into your `case object`.
3. Still in `parseTransform`, if the name is not in the `match`, VerCors
   throws. The text of that error is:

   `Use add-if-zero, add-if-one, for-to-while, deepen-while, or to-method`

   Change it so the list also contains `my-transform`. This does not run
   the rewrite. It only updates the message someone sees after a typo.

Do not add your rewriter to `Transformation.scala`. That file is the
rewrite list for verification. If you put it there, every verify would
apply the mutant, not only `--robustness`.

Which transform runs is the string `TRANSFORM` in `config.env` (the
driver copies it to `ROBUSTNESS_TRANSFORM`). `Options.scala` already has
`--robustness`. You do not add a new flag for each transform.

## 5. Mixed mode

Skip this if you will not use `MODE=mixed`.

File: [RobustnessSpin.scala](src/rewrite/vct/rewrite/RobustnessSpin.scala).

- `Kind` defines a transformation's label, candidate count and application.
- `AddIfZeroKind` implements that interface for add-if-zero.
- `kindOf` maps a registered rewriter builder to its `Kind`.

Copy `AddIfZeroKind` and the `kindOf` line:

1. After `AddIfZeroKind`, add `MyTransformKind` the same way. `label` is
   `"my-transform"`. `candidateCount` is
   `MyTransformSites.collect(program).size`. `apply` is
   `MyTransform[Pre]().dispatch(program)`.
2. In `kindOf`, next to `case AddIfZero => AddIfZeroKind`, add
   `case MyTransform => MyTransformKind`.

When you want mixed to pick it, put `my-transform` on `TRANSFORM=` in
`config.env`.

If `TRANSFORM` is empty, `selectedBuilders` in
[Robustness.scala](src/main/vct/main/modes/Robustness.scala) selects the four
earlier transformations for mixed mode. `RobustnessSpin.defaultPool` has the
same four kinds as its fallback. Update both defaults only if the new
transformation should be included without an explicit `TRANSFORM` setting.

## 6. Driver script

If `run-benchmark.sh` should accept the name: add `MY_TRANSFORM_SITE` to
`ALL_SITE_VARS`, add the name in `site_var_for` and in `usage`, and set
`TRANSFORM` in `config.env`. Mixed also needs step 5.

Keep the candidate-count and completion messages used by `candidate_count`
and `transform_status` in [run-benchmark.sh](util/robustness/run-benchmark.sh).
The driver discovers the site count from `candidate count = <number>` in the
transformation log.

## 7. Check the new transformation

Use a verified original program with an applicable site and inspect the generated
C and annotations as well as its verification log. Check the transformation's
control-flow, variable-update and permission handling, including cases outside
the initial example. An input with no applicable site should remain unchanged.

For benchmark validation, use `VERIFY_ORIGINALS=1`, `VERIFY_MUTANTS=1` and a new
output directory. `FORCE_ORIGINAL=1` forces original verification if logs already
exist; otherwise the driver can reuse them. Rerun the relevant existing cases
after shared-code changes to check for regressions. Report a mutant as ROBUST
only when both verifications succeed and the transformation preserves the
program's meaning and annotation obligations. Inspect failure logs before
classifying an unsuccessful result as NOT_ROBUST.

## Existing example: to-method

The current implementation is in
[ToMethodSites.scala](src/rewrite/vct/rewrite/ToMethodSites.scala) and
[RobustnessToMethod.scala](src/rewrite/vct/rewrite/RobustnessToMethod.scala).
The collector chooses a nonempty block without declarations, nested loops,
nonlocal control transfers, `switch`, `send`, or `recv`. The rewriter captures
variables used from the surrounding scope, creates a helper procedure and
replaces the block with a call. Names bound within quantifiers and `\let`
expressions are excluded from the parameter list.

This example has two verification strategies. A restricted set of independent
assignments to captured scalar locals receives generated pointer permissions
and postconditions using `\old`; its helper is checked separately. Other
supported blocks use VerCors's existing `inline` mechanism, which expands the
helper body at each call before backend verification. The latter is not a
general algorithm for synthesizing a helper contract. The exact conditions,
including the return-value path and pointer captures, are described in
[the current implementation notes](robustness-changes.md#current-to-method-implementation).

When adding or extending an extraction transform, check the generated parameters,
the caller's updated variables, and the annotations as well as the final verdict.
`--robustness` writes C; it does not itself verify that C. The benchmark driver
must verify the original and invoke VerCors again on the generated file. A parse,
resolution or tool failure is not a `NOT_ROBUST` result.

The [to-method selection report](robustness-benchmarks/initial-run/to-method/to_method_selected_benchmark.md)
records the input criteria and exclusions. The
[to-method results report](robustness-benchmarks/latest-recheck/to-method/to_method_experiment_results_summary.md)
links each input, output and log, and labels whether the generated helper is inline.
See [the source map](robustness-files.md#to-method-source-map) for the shared C-output
files involved; the transformer implementation is Scala, while the C files are
experiment inputs and generated outputs.
