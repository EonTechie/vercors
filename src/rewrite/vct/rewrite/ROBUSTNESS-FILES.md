# What we built

This file is the map of the work on `robustness-post-resolution`: which
files we added or changed, and what each one is for, so the big picture
is visible. Deleted experiments and flags are not listed here; their
full text is in
[`ROBUSTNESS-CHANGES.md`](ROBUSTNESS-CHANGES.md).

It is **not** a recipe for a sixth transform. That guide is
[`ROBUSTNESS.md`](ROBUSTNESS.md).

Two jobs were done, and they must stay separate in your head:

1. A robustness **engine**: a VerCors mode, five rewrites, site lists, and
   spin. Those rewrites are not on the ordinary Verify list in
   `Transformation.scala`.
2. **Parseable C dumps**: printing a resolved COL program as `.c` used to
   leak COL syntax. Shared printers were changed. There is no
   robustness-only `if`. Any `Ctx.C` (also Cuda / OpenCL / CPP) program
   print uses them. `--trace-col` uses default `Ctx()` = PVL, so it does
   not use that path.

`vercors --robustness` does not run Silicon. The driver
`util/robustness/run-benchmark.sh` verifies originals and mutants with
ordinary `vercors` when `VERIFY_ORIGINALS` and `VERIFY_MUTANTS` are 1.

Names in use: `add-if-zero`, `add-if-one`, `for-to-while`, `deepen-while`,
`to-method`. Underscores (`add_if_zero`) are accepted. The old SemTransforms
names (`if0error`, `add_if1`, `for2while`) are not.

---

## Added files

These paths did not exist on `origin/dev`.

### Mode

| File | What it is responsible for |
|---|---|
| `src/main/vct/main/modes/Robustness.scala` | The `--robustness` pipeline: parse, resolve, apply the chosen transform(s), print C to `robustness-transformed.c`. Which transform runs is the string in the environment variable `ROBUSTNESS_TRANSFORM` (`add-if-zero`, `add-if-one`, `for-to-while`, `deepen-while`, `to-method`). The driver copies `TRANSFORM` from `config.env` into that variable. Repeat / spin / seed come from `--robustness-repeat`, `--robustness-spin`, `--robustness-seed`. |

### Rewrites (they change the tree)

None of these run during ordinary Verify.

| File | Name | What it does |
|---|---|---|
| `src/rewrite/vct/rewrite/AddIfZero.scala` | `add-if-zero` | Wraps one statement as `if (0) { assert false } else { S }`. |
| `src/rewrite/vct/rewrite/AddIfOne.scala` | `add-if-one` | Same places as AddIfZero. Wraps as `if (1) { S } else { assert false }`. |
| `src/rewrite/vct/rewrite/RobustnessForLoopToWhileLoop.scala` | `for-to-while` | Turns one selected `for` into a while by calling `ForLoopToWhileLoop.rewriteLoop`. |
| `src/rewrite/vct/rewrite/RobustnessDeepenWhile.scala` | `deepen-while` | Turns `while (c) S` into an outer while plus inner `while (nondet() && c) S`. |
| `src/rewrite/vct/rewrite/RobustnessToMethod.scala` | `to-method` | Moves the selected block into a new procedure. |

If `ROBUSTNESS_TRANSFORM` is unset, spin uses add-if-zero, add-if-one,
for-to-while, and deepen-while. `to-method` is used only when you list it.

### Place lists (they say where a rewrite may touch)

| File | Used by | What it collects |
|---|---|---|
| `src/rewrite/vct/rewrite/CStatementSites.scala` | add-if-zero, add-if-one | Wrappable C statements. |
| `src/rewrite/vct/rewrite/ForLoopToWhileLoopSites.scala` | for-to-while | `for` loops that do not contain `continue`. |
| `src/rewrite/vct/rewrite/DeepenWhileSites.scala` | deepen-while | `while` loops with `LoopInvariant`. Skips the generated inner adapter. |
| `src/rewrite/vct/rewrite/ToMethodSites.scala` | to-method | Blocks that can be extracted into a procedure. |

### Engine helpers

| File | What it is responsible for |
|---|---|
| `src/rewrite/vct/rewrite/RobustnessSiteSelection.scala` | Returns which index the rewriter should use on this application, after the matching `*Sites` collector has already built the candidate list. Preferred and multiple modes take that index from the transform's environment variable (`ADD_IF_ZERO_SITE`, `ADD_IF_ONE_SITE`, `FOR_TO_WHILE_SITE`, `DEEPEN_WHILE_SITE`, `TO_METHOD_SITE`). If the variable is unset, the index is 0. With `--robustness-spin` (the driver's mixed mode), `RobustnessSpin` draws the random index and stores it with `forceNext`; the following `nextIndex` call uses that stored number instead of the environment variable. |
| `src/rewrite/vct/rewrite/RobustnessSpin.scala` | Implements one `--robustness-spin` step. It first picks a transform from the pool (each remaining kind with at least one site has the same chance), then picks a site among that kind's current candidates (each candidate has the same chance). |
| `src/rewrite/vct/rewrite/RobustnessStatementWrap.scala` | Shared wrap helpers for add-if-zero, add-if-one, deepen-while, and to-method. `asCompound` puts a statement inside a `{ }` block. `keepSurroundingBraces` keeps a wrapped site as a `{ }` block when the original node was already a `Block` or `Scope`, so a loop or if body that was compound stays compound after the wrap. |

### Driver and these two documents

These are not VerCors core.

| File | What it is responsible for |
|---|---|
| `src/rewrite/vct/rewrite/ROBUSTNESS-FILES.md` | The map of the work on this branch: which files were added, changed, or removed, and what each one is for, so the big picture is visible without opening every source. |
| `src/rewrite/vct/rewrite/ROBUSTNESS-CHANGES.md` | After this work forked from VerCors (`origin/dev`), this file shows those changes in full: every new file's source, every removed file or flag with the source that used to be there, and before/after fragments for files that already existed. |
| `src/rewrite/vct/rewrite/ROBUSTNESS.md` | Instruction guide for adding a transform that does not exist yet. |
| `util/robustness/run-benchmark.sh` | Runs the automated robustness pipeline. It calls ordinary `vercors` on originals, then `--robustness`, then ordinary `vercors` on mutants, and writes TSV verdicts. From the repository root: `./util/robustness/run-benchmark.sh`. |
| `util/robustness/config.env` | The settings the script reads. Edit this file, then run the script. |

---

## Changed existing files

These paths already existed. We did not delete and rewrite them. We added
or extracted the pieces below.

### Hook the mode into VerCors

| File | What we added | What that addition is responsible for |
|---|---|---|
| `src/main/vct/options/types/Mode.scala` | `case object Robustness` | The mode name. |
| `src/main/vct/main/Main.scala` | `Mode.Robustness => Robustness.runOptions` | `--robustness` starts the new mode. |
| `src/main/vct/options/Options.scala` | `--robustness`, `--robustness-repeat`, `--robustness-spin`, `--robustness-seed` | CLI flags. `--robustness` does not run Silicon. |

### Share the existing for → while algorithm

| File | What we changed | What that change is responsible for |
|---|---|---|
| `src/rewrite/vct/rewrite/ForLoopToWhileLoop.scala` | The for → while conversion used to live only inside Verify’s rewriter. We moved it into a function named `rewriteLoop`. Verify’s rewriter now calls that function. | Ordinary Verify still converts every `for`. Robustness for-to-while converts one selected `for` by calling the same function, so the algorithm is not copied. |

### Hide generated sizeof helpers in C dumps

VerCors invents a helper function for `sizeof` (for example `sizeof_int`).
The verifier needs that helper. A real C file does not contain it, so if
the dump prints it, `vercors` cannot parse the dump as C. The helper
stays in the tree. The C program printer does not write its declaration.

| File | What we added or changed |
|---|---|
| `src/col/vct/col/origin/Origin.scala` | Adds a label, `GeneratedSizeOfHelper`, that marks a sizeof helper. |
| `src/rewrite/vct/rewrite/lang/LangCToCol.scala` | Creates the helper and puts that label on it. |
| `src/col/vct/col/ast/declaration/global/FunctionImpl.scala` | Answers whether a function is a sizeof helper: first the label, then a name that starts with `sizeof_` or `size_of_`. |
| `src/col/vct/col/ast/node/ProgramImpl.scala` | When writing the program as C, skips those helpers and prints the other declarations. |

`--trace-col` does not use this C printer, so the helper still shows up
there.

### Shared C pretty-print (so a dump parses as C)

After resolve, printing the tree as `.c` used to leak COL syntax, so
`vercors` could not parse the dump. We only changed how that tree is
written as C. The PVL, Silver, and Java printers were not edited. Silicon
does not see this; it is only the C text of the dump. Each of these C
print changes has a comment that starts with
`Post-resolution C pretty-print`, so you can find them next to the
original VerCors comments.

| File | What the C layout now does |
|---|---|
| `src/col/vct/col/print/Ctx.scala` | Turns names into legal C identifiers (letters, digits, `_`). |
| `TPointerArrayImpl.scala` and `TNonNull*` / `TConst*` siblings | Writes pointer arrays as `T*`, not `int[]`. |
| `TArrayImpl.scala` | Writes a C array parameter as a pointer, the way C does. |
| `TBoolImpl.scala` | Writes the C boolean type as `_Bool`. Cuda and C++ still write `bool`. |
| `TClassImpl.scala` | Writes a by-value class type as `struct T`. |
| `ClassImpl.scala` | Writes a by-value class body as `struct { ... };`. |
| `ProcedureImpl.scala` | Writes the procedure’s contract (`requires` / `ensures`) in the C dump. |
| `LoopImpl.scala` | Writes `for (int i = e; ...)` as one header, not a block around the loop. |
| `BooleanValueImpl.scala` | Writes `1` and `0` in executable C. Specifications still write `true` and `false`. |
| `NullImpl.scala` | Writes `NULL` instead of `null`. |

---

## What we did not do, on purpose

| Choice | Why |
|---|---|
| Do not delete sizeof helpers from the AST | Pointer-size encoding uses them. |
| Do not hide them in `FunctionImpl.layout` | The helper is still in use in the tree. Hiding it when a single function is printed would hide it everywhere. We only skip it when writing the whole program as C (`ProgramImpl`). |
| Do not put `to-method` in the default spin pool | List it in `ROBUSTNESS_TRANSFORM` / `TRANSFORM` when you want it. |

---

## Not implementation

| Path | What it is |
|---|---|
| `robustness-results/post-resolution/` | Driver output: mutants, logs, TSV. The script writes here. |
| `robustness-transformed.c` in the working directory | Last `--robustness` dump. Do not commit it. |

---

## How to run what already exists

Edit `util/robustness/config.env`, then:

```bash
./util/robustness/run-benchmark.sh
```

Dump only, no Silicon:

```bash
ROBUSTNESS_TRANSFORM=add-if-zero \
  out/vercors/main/runScript.dest/vercors --robustness file.c
```

Use `out/vercors/main/runScript.dest/vercors` while mill is compiling, not
`bin/vct`.

---

## If you need to change something later

| You want to | Open |
|---|---|
| Add a transform that does not exist yet | [`ROBUSTNESS.md`](ROBUSTNESS.md) |
| Change which statements a rewrite may touch | The matching `*Sites.scala` |
| Change repeat / spin / seed | `Robustness.scala` and `Options.scala` |
| Change the dump path or C syntax of the mode | `Robustness.scala` `Output(...)` |
| Hide sizeof helpers in C | The four files in the sizeof section; not `FunctionImpl.layout` |
| Fix invalid C in a dump | The pretty-print files in that section |
| Change a folder run | `util/robustness/config.env`, then the script |
