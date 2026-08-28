# Adding a robustness transform

## What `--robustness` does

VerCors reads the C file, resolves names and types, then runs the chosen
rewrite one or more times on that tree. It writes `robustness-transformed.c`
in the directory you ran from. It does not run Silicon. To check the dump,
run ordinary `vercors` on `robustness-transformed.c`.

`--robustness` is a mode (`Mode.Robustness` in `Mode.scala`, handled in
`Main.scala`). It is not `--dev-add-if-zero`. That hidden option, if passed,
puts AddIfZero into the normal Verify rewrite list in `Transformation.scala`.
Robustness does not read it. A new transform does not go there.

## Two parts of one transform

1. A list of places in the tree that this rewrite is allowed to touch.
   AddIfZero and AddIfOne both call `CStatementSites.collect`.
   For-to-while calls `ForLoopToWhileLoopSites.collect`.
2. A `Rewriter` that changes exactly one of those places: the index from
   `RobustnessSiteSelection.nextIndex`. If that env var is unset, the index
   is 0. The place is the tree node itself (`eq`), not a line number and not
   the printed C text.

`--robustness-repeat N` runs that rewrite N times on the same tree.
`--robustness-spin` picks a rewrite name and an index at random each time
(`RobustnessSpin.scala`). Without spin, `ROBUSTNESS_TRANSFORM` chooses the
name (comma-separated names cycle). Default without the env var is AddIfZero.

AddIfZero and AddIfOne use the same place-list because they both put a
statement inside an `if`. They only differ in the `if` they build.
For-to-while cannot use that list: it only accepts `for` loops without
`continue`. It calls `ForLoopToWhileLoop.rewriteLoop`.

AddIfOne extends `CSourceRewriter`. That is leftover. A new transform should
extend `Rewriter`, like AddIfZero.

If you replace a `{ ... }` body with an `if`, call
`RobustnessStatementWrap.keepSurroundingBraces`. Otherwise the printed C can
look like `for (...) if (0)` with no braces around the loop body.

After resolve, `int x = e` is not a place in `CStatementSites`.
`return` / `goto` / `break` / `continue` themselves are not places; a parent
`if` or loop still can be.

## Current names

| `ROBUSTNESS_TRANSFORM` | Place list | What it does |
|---|---|---|
| `add-if-zero` (`if0error`) | `CStatementSites` | `if (0) { assert false } else { S }` |
| `add-if-one` (`add_if1`) | `CStatementSites` | `if (1) { S } else { assert false }` |
| `for-to-while` (`for2while`) | `ForLoopToWhileLoopSites` | `ForLoopToWhileLoop.rewriteLoop` |
| `deepen-while` (`deepen_while`) | `DeepenWhileSites` | nest `while (c) { while (nondet() && c) S }`. Only `while` with `LoopInvariant`. Outer drops `decreases`; inner keeps it. Only the inner adapter is excluded from later DeepenWhile collects; the outer `while (C)` stays eligible |

`if0error`, `add_if1`, `for2while` only rename the VerCors rewrites. They are
not the SemTransforms Python inserts.

## How to run

```bash
ROBUSTNESS_TRANSFORM=add-if-zero \
  out/vercors/main/runScript.dest/vercors --robustness --robustness-repeat 5 file.c

ADD_IF_ZERO_SITE=2 \
  out/vercors/main/runScript.dest/vercors --robustness file.c

out/vercors/main/runScript.dest/vercors \
  --robustness --robustness-spin --robustness-repeat 5 --robustness-seed 42 file.c
```

Use `out/vercors/main/runScript.dest/vercors`, not `bin/vct`, while mill is
compiling. Unset leftover `*_SITE` env vars first.

## Steps to add a new transform

Copy `AddIfZero.scala`. If the change is `for` → `while`, copy
`RobustnessForLoopToWhileLoop.scala` instead.

1. Place list. Same statements as AddIfZero → `CStatementSites.collect`.
   Same `for` loops as for-to-while → `ForLoopToWhileLoopSites.collect`.
   Otherwise a new `collect` in `src/rewrite/vct/rewrite/`, visiting
   `Procedure` and `CFunctionDefinition` like those two files, skipping
   `reach_error`, `abort`, `__VERIFIER_*`. The three transforms that exist
   all pick a `Statement` and override `dispatch(stat)`.

2. Rewriter file in `src/rewrite/vct/rewrite/`.
   `case object Foo extends RewriterBuilder` and
   `case class Foo[Pre <: Generation]() extends Rewriter[Pre]`.
   In `dispatch(program)`: `collect`, then
   `RobustnessSiteSelection.nextIndex("FOO_SITE", candidates.size, required = false)`,
   then `super.dispatch`.
   In `dispatch(stat)`: if `selected.exists(_.target eq stat)`, change that
   node once; else `rewriteDefault`.

3. `src/main/vct/main/modes/Robustness.scala`: import; a case in
   `parseTransform` (this is `ROBUSTNESS_TRANSFORM=...`); and if
   `--robustness-spin` with no `ROBUSTNESS_TRANSFORM` should include it, the
   `Seq` inside `selectedBuilders` when spin is true (today: AddIfZero,
   AddIfOne, RobustnessForLoopToWhileLoop).

4. `src/rewrite/vct/rewrite/RobustnessSpin.scala`, only for
   `--robustness-spin`: a `Kind` (`candidateCount` = the `collect` from step 1,
   `apply` = the rewriter from step 2); a case in `kindOf`; `defaultPool` if
   it should run when `ROBUSTNESS_TRANSFORM` is unset. Skip this file and
   `ROBUSTNESS_TRANSFORM=your-name` still works after step 3.
   `--robustness-spin` then errors with
   `Robustness spin pool cannot include …`.

5. `./mill vercors.main.compile`, then
   `out/vercors/main/runScript.dest/vercors`. A new file under
   `src/rewrite/vct/rewrite/` compiled without changing `build.sc` when
   `RobustnessSpin.scala` was added. There is no robustness test directory.
   There is no extra flag in `Options.scala` for AddIfZero / AddIfOne /
   for-to-while; the name is `ROBUSTNESS_TRANSFORM`, the place is the string
   given to `nextIndex`. Do not add the new rewriter to
   `Transformation.scala`.
