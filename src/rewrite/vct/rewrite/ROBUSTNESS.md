# Adding a robustness transform that does not exist yet

This file is an instruction for wiring a **new** rewrite into
`--robustness`. It is not a catalogue of the five rewrites that already
exist. Those are recorded in [`ROBUSTNESS-FILES.md`](ROBUSTNESS-FILES.md).

`--robustness` does not run Silicon. After a dump exists, verification is
a separate ordinary `vercors` invocation, or the driver script.

Do not add the new rewrite to `Transformation.scala`. That object is the
ordinary Verify pass list. None of the robustness rewrites are on it.

Do not add a new flag in `Options.scala`. The name is the environment
variable `ROBUSTNESS_TRANSFORM`. The site index is a second environment
variable that you pass to `RobustnessSiteSelection.nextIndex`.

---

## What the engine requires

A robustness transform is three things that must agree:

1. A **collector** that returns the AST nodes this rewrite is allowed to
   change, in a stable order, so index `0` is always the first node in that
   list.
2. A **rewriter** (`RewriterBuilder` + `Rewriter`) that, on each
   application, asks `RobustnessSiteSelection.nextIndex` for one index,
   changes that node once, and leaves every other node to
   `rewriteDefault`.
3. A **name** in `parseTransform` in
   `src/main/vct/main/modes/Robustness.scala`, so
   `ROBUSTNESS_TRANSFORM=<name>` selects that builder.

The selected node is identified by reference (`eq` on the tree node), not
by line number and not by printed C text.

If the collector returns no nodes, `nextIndex(env, 0)` returns `None` and
the rewriter must not change the program. Call `nextIndex` even in that
case: spin may have stored a forced index, and that call consumes it.

---

## 1. Choose the two strings

Pick the string that will appear in `ROBUSTNESS_TRANSFORM`, and the
environment variable that will hold the site index.

Example: name `my-transform`, site variable `MY_TRANSFORM_SITE`.

`parseTransform` matches on `name.trim.toLowerCase`. Put in the `case`
exactly the strings you will export. If you only list `"my-transform"`,
then `ROBUSTNESS_TRANSFORM=my_transform` is unknown and the process stops
with `Unknown ROBUSTNESS_TRANSFORM=...`.

Update the error text in that same `match` so it lists the new name.
Otherwise a typo still tells the user only the old five names.

`RewriterBuilder.key` is a separate Scala string (used in logs and in the
spin error `Robustness spin pool cannot include …`). It must not collide
with another builder’s `key` in the same run.

---

## 2. Write the collector

Add a Scala object whose `collect(program)` returns the allowed nodes.

After `--robustness` has parsed and resolved, C functions appear as
`CFunctionDefinition` or `Procedure` under `program.declarations`,
sometimes nested in `CTranslationUnit`. Every collector that exists today
walks those declarations and skips function names `reach_error`, `abort`,
`assume_abort_if_not`, and names that start with `__VERIFIER_`. Do the
same unless you have a documented reason not to: otherwise you will
rewrite error-sink helpers.

Which **node type** you collect is the definition of this transform. That
decision is yours. Do not call another transform’s collector unless that
list is exactly the set of nodes you intend to change. A wrong collector
selects the wrong node and the rewrite is still “successful” as far as
the engine is concerned.

Each site must hold the target node (for `eq`) and whatever you need to
log. The engine only cares that `candidates(index).target` is the node
the rewriter will test with `eq`.

---

## 3. Write the rewriter

Add a `RewriterBuilder` and a `Rewriter[Pre]` in
`src/rewrite/vct/rewrite/`. The `rewrite` mill module takes all Scala
under `src/rewrite`. `build.sc` does not list individual rewriter files.

In `dispatch(program: Program[Pre])`:

1. `val candidates = YourSites.collect(program)`
2. `selected = RobustnessSiteSelection.nextIndex("YOUR_SITE", candidates.size)`
   mapped to `candidates(index)` when the `Option` is `Some`
3. `super.dispatch(program)`

In `dispatch` for the node kind you change: if the stored target `eq` this
node, apply the AST edit once; otherwise `rewriteDefault`.

The AST edit is the meaning of this transform. The engine does not
constrain it beyond: one selected node per application, identity by `eq`.

Printer constraint, verified in `RobustnessStatementWrap` and the C loop
printer: `if` and `for`/`while` only emit `{ }` when the body is a
`Block` or `Scope`. If you replace a `Block`/`Scope` body with a node that
is neither, the dump can drop the braces. In that situation call
`RobustnessStatementWrap.keepSurroundingBraces(original, replacement)`.
If the replacement is already a `Block` or `Scope`, you do not need it.

---

## 4. Register the name (required for any use)

In `src/main/vct/main/modes/Robustness.scala`:

- import the `RewriterBuilder`
- add a `case` in `parseTransform`
- add the name to the `Unknown ROBUSTNESS_TRANSFORM` message

After a compile, this is enough for a non-spin run:

```bash
ROBUSTNESS_TRANSFORM=my-transform \
  out/vercors/main/runScript.dest/vercors --robustness file.c
```

`file.c` must actually contain at least one collected site. If it does
not, the dump is the resolved program with no rewrite applied.

Compile:

```bash
./mill vercors.main.compile
```

Use `out/vercors/main/runScript.dest/vercors`, not `bin/vct`, while that
compile is the binary you intend to run.

---

## 5. Register spin (`kindOf`) if the name will be used with `--robustness-spin`

Non-spin runs never call `RobustnessSpin.kindOf`.

`--robustness-spin` (and the driver’s `MODE=mixed`) builds the pool with
`builders.map(RobustnessSpin.kindOf)`. If `kindOf` has no case for the
new builder, the process throws:

`Robustness spin pool cannot include <key>`

In `src/rewrite/vct/rewrite/RobustnessSpin.scala`:

- a `Kind` with `label` equal to the `ROBUSTNESS_TRANSFORM` name,
  `candidateCount` equal to `YourSites.collect(program).size`,
  `apply` equal to `YourRewriter[Pre]().dispatch(program)`
- `case YourRewriter => YourKind` in `kindOf`

Default pool when `ROBUSTNESS_TRANSFORM` is **unset** and spin is on is
the `Seq` in `selectedBuilders` in `Robustness.scala` (currently four
builders; not `to-method`). Put the new builder in that `Seq` only if an
unset env var must include it. If the driver will list the name in
`TRANSFORM`, you do not add it to that default `Seq`.

`RobustnessSpin.defaultPool` is used only when `kindOf` is given an empty
list. The mode does not pass an empty list when `selectedBuilders`
returns the four-builder `Seq`. Do not edit `defaultPool` instead of
`kindOf` and expect `MODE=mixed` with `TRANSFORM=my-transform` to work.

---

## 6. Register the driver if `run-benchmark.sh` must accept the name

`vercors --robustness` does not read the script. The script is required
only for folder runs.

In `util/robustness/run-benchmark.sh`:

- add `YOUR_SITE` to `ALL_SITE_VARS` (otherwise a leftover value in the
  shell is not cleared)
- add the name in `site_var_for` so it prints `YOUR_SITE`
- add the name on the `--transform` line in `usage`

`preferred` calls `site_var_for` and stops with `unknown transform` if
that function returns empty.

In `util/robustness/config.env`, set `TRANSFORM` to the new name when you
want that run. Mixed uses that list as the spin pool, so step 5 must
already be done.

---

## 7. Check the dump

```bash
./mill vercors.main.compile

unset YOUR_SITE
ROBUSTNESS_TRANSFORM=my-transform \
  out/vercors/main/runScript.dest/vercors --robustness file.c
```

The mode writes `robustness-transformed.c` in the current working
directory. Inspect that file. Site `0` is the first element of your
collector’s list for that program.

A later index:

```bash
YOUR_SITE=1 \
ROBUSTNESS_TRANSFORM=my-transform \
  out/vercors/main/runScript.dest/vercors --robustness file.c
```

If the collector has fewer than two sites, `YOUR_SITE=1` throws
`Invalid YOUR_SITE index 1` with the valid range in the message.

Verification of the dump is not `--robustness`:

```bash
out/vercors/main/runScript.dest/vercors robustness-transformed.c
```

There is no robustness test directory in this repository.

---

## Do not

- Do not put the rewriter on the Verify list in `Transformation.scala`.
- Do not add `--my-transform` in `Options.scala`.
- Do not reuse another transform’s collector unless the allowed nodes are
  the same set.
- Do not skip `kindOf` if the name will appear in a spin pool.
- Do not skip `ALL_SITE_VARS` if the driver will set `YOUR_SITE`.
