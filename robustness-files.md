# What we built

This document maps the implementation files on `robustness-post-resolution`
to their responsibilities. [robustness-changes.md](robustness-changes.md)
explains the changes, and [robustness.md](robustness.md) gives the steps for
adding another transformation.

The implementation has two parts:

1. A robustness **engine**: a VerCors mode, five rewrites, site lists, and
   spin. Those rewrites are not on the ordinary Verify list in
   `Transformation.scala`.
2. **C output support**: shared AST printers emit the resolved program as
   annotated C for a second VerCors invocation. These changes are selected by
   the printing context (`Ctx.C`, and in several printers the related Cuda,
   OpenCL and CPP contexts). PVL trace output uses a different context.

`vercors --robustness` does not run Silicon. The driver
`util/robustness/run-benchmark.sh` verifies originals and mutants with
ordinary `vercors` when `VERIFY_ORIGINALS` and `VERIFY_MUTANTS` are 1.

Names in use: `add-if-zero`, `add-if-one`, `for-to-while`, `deepen-while`,
`to-method`. Underscores (`add_if_zero`) are accepted. The old SemTransforms
names (`if0error`, `add_if1`, `for2while`) are not.

## Current benchmark state

- `to-method`: both the initial experiment and latest recheck record 100 ROBUST
  results across 19 programs and 100 sites. The
  [latest results table](robustness-benchmarks/latest-recheck/to-method/to_method_experiment_results_summary.md)
  links the inputs, outputs and verification logs.
- The four earlier transformations use the same
  [34 selected programs](robustness-benchmarks/initial-run/earlier-four-transformations/broader-benchmark-selection.md).
  Their [latest recheck](robustness-benchmarks/latest-recheck/benchmark-recheck.md)
  records 347 ROBUST results for add-if-zero, 347 for add-if-one and 9 for
  for-to-while: 703 in total. Deepen-while has no applicable sites in this set.
  The corresponding initial ROBUST counts were 180, 180 and 1.
- The remaining failure classifications are under review. In particular, the
  eight for-to-while rows labelled NOT_ROBUST in the latest raw table report
  iteration-contract processing errors; they are not valid NOT_ROBUST results.
  The [benchmark overview](robustness-benchmarks/benchmark-overview.md) explains
  this distinction and links both runs. Repeated and mixed pilot tests are
  separate and were not rerun in the latest recheck.

---

## To-method source map

The extraction algorithm is implemented in Scala. The selected `.c` files are
copied inputs and generated outputs; they are not the implementation source.
This map covers the to-method logic and the shared output support relevant to
its recorded examples. Shared files also serve other transformations.

| File | Current role for to-method |
|---|---|
| [ToMethodSites.scala](src/rewrite/vct/rewrite/ToMethodSites.scala) | Structural block selection; free-local and heap-local capture; excludes quantifier/let-bound names from captures. |
| [RobustnessToMethod.scala](src/rewrite/vct/rewrite/RobustnessToMethod.scala) | Helper parameters, call replacement, pointer or return-value propagation, restricted contract generation, inline decision, loop permission framing and surrounding send/recv declaration scope. |
| [RobustnessStatementWrap.scala](src/rewrite/vct/rewrite/RobustnessStatementWrap.scala) | Keeps compound-statement braces when replacing a block with a call. |
| [RobustnessSiteSelection.scala](src/rewrite/vct/rewrite/RobustnessSiteSelection.scala) | Reads and validates the selected site index (`TO_METHOD_SITE`) through the shared selection mechanism. |
| [Robustness.scala](src/main/vct/main/modes/Robustness.scala) | Registers to-method and runs parse, resolution, transformation and C output. |
| [RobustnessSpin.scala](src/rewrite/vct/rewrite/RobustnessSpin.scala) | Makes to-method available when explicitly included in the mixed-mode pool; it is not in the default four-transform pool. |
| [run-benchmark.sh](util/robustness/run-benchmark.sh) | Discovers sites, invokes transformation and separate original/output verification, and records artifacts. |
| [ProcedureImpl.scala](src/col/vct/col/ast/declaration/global/ProcedureImpl.scala) | Prints generated procedure declarations, contracts and the `inline` marker as C. |
| [LocalHeapVariableImpl.scala](src/col/vct/col/ast/declaration/singular/LocalHeapVariableImpl.scala) | Prints address-taken local storage and initializes non-null storage through `vercorsMallocNonNull`. |
| [NewNonNullPointerImpl.scala](src/col/vct/col/ast/expr/heap/alloc/NewNonNullPointerImpl.scala), [NewPointerArrayImpl.scala](src/col/vct/col/ast/expr/heap/alloc/NewPointerArrayImpl.scala) | Emit non-null allocation in a form that the C front end can recognize. This is not a claim of complete multidimensional-array support. |
| [ProgramImpl.scala](src/col/vct/col/ast/node/ProgramImpl.scala) | Includes declarations needed by generated C, including allocation helpers. |
| [LangCToCol.scala](src/rewrite/vct/rewrite/lang/LangCToCol.scala) | Reconstructs non-null allocation from the recognized generated helper call. |
| [TUniqueImpl.scala](src/col/vct/col/ast/type/TUniqueImpl.scala) | Emits the uniqueness qualifier inside a C specification annotation. |
| [AmbiguousDivImpl.scala](src/col/vct/col/ast/expr/op/num/AmbiguousDivImpl.scala), [FloorDivImpl.scala](src/col/vct/col/ast/expr/op/num/FloorDivImpl.scala), [AmbiguousModImpl.scala](src/col/vct/col/ast/expr/op/num/AmbiguousModImpl.scala), [ModImpl.scala](src/col/vct/col/ast/expr/op/num/ModImpl.scala) | Print Euclidean division/remainder AST nodes with their VerCors notation. |
| [HeapVariableImpl.scala](src/col/vct/col/ast/declaration/global/HeapVariableImpl.scala) | Removes the internal storage-pointer layer when printing mutable C globals. |

[InlineApplicables.scala](src/rewrite/vct/rewrite/InlineApplicables.scala) is an
existing VerCors mechanism used by the generated inline helpers, not a new
to-method transformation. [The implementation notes](robustness-changes.md#current-to-method-implementation)
explain the two helper-verification paths and their limits.

The [selected benchmark](robustness-benchmarks/initial-run/to-method/to_method_selected_benchmark.md)
and [experiment results summary](robustness-benchmarks/latest-recheck/to-method/to_method_experiment_results_summary.md)
provide the input inventory, exclusions and per-site artifact links.

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

### Driver and these documents

These are not VerCors core.

| File | What it is responsible for |
|---|---|
| `robustness-files.md` | The map of the implementation, benchmark layout, and the file responsible for each part. |
| `robustness-changes.md` | The detailed record of changes relative to upstream VerCors. |
| `robustness.md` | Instructions for adding a transform that does not exist yet. |
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

### Generated sizeof helpers and declarations

VerCors represents some `sizeof` operations using generated specification
functions such as `sizeof_int`. The current C output retains these declarations
so their uses in annotations can resolve when the output is read again.

| File | What we added or changed |
|---|---|
| `src/col/vct/col/origin/Origin.scala` | Adds a label, `GeneratedSizeOfHelper`, that marks a sizeof helper. |
| `src/rewrite/vct/rewrite/lang/LangCToCol.scala` | Creates the helper and puts that label on it. |
| [FunctionImpl.scala](src/col/vct/col/ast/declaration/global/FunctionImpl.scala) | Prints functions as specification declarations in C output. It retains `isGeneratedSizeOfHelper`, but the current program printer does not use that predicate to filter declarations. |
| [ProgramImpl.scala](src/col/vct/col/ast/node/ProgramImpl.scala) | Prints all program declarations and supplies missing declarations for the generated allocation/free helpers. |

### Shared C pretty-print (so a dump parses as C)

The C layouts translate resolved COL constructs into syntax accepted by the
C front end. They are shared by robustness output and other uses of the C
printer. The generated file is parsed and verified in a separate invocation;
the to-method source map above also covers the associated C-to-COL changes.

| File | What the C layout now does |
|---|---|
| `src/col/vct/col/print/Ctx.scala` | Replaces punctuation in generated names with underscores and prefixes empty or digit-leading names in C-family output. |
| `TPointerArrayImpl.scala` and `TNonNull*` / `TConst*` siblings | Writes pointer arrays as `T*`, not `int[]`. |
| `TArrayImpl.scala` | Writes a C array parameter as a pointer, the way C does. |
| `TBoolImpl.scala` | Writes the C boolean type as `_Bool`. Cuda and C++ still write `bool`. |
| `TClassImpl.scala` | Writes a by-value class type as `struct T`. |
| `ClassImpl.scala` | Writes a by-value class body as `struct { ... };`. |
| `ProcedureImpl.scala` | Writes the procedure’s contract (`requires` / `ensures`) in the C dump. |
| `LoopImpl.scala` | Writes `for (int i = e; ...)` as one header, not a block around the loop. |
| `BooleanValueImpl.scala` | Writes `1` and `0` in executable C. Specifications still write `true` and `false`. |
| `NullImpl.scala` | Writes `NULL` instead of `null`. |

The broader C-output support also includes vectors: compound literals are
handled in [CToCol.scala](src/parsers/vct/parsers/transform/CToCol.scala), and
[CoercionUtils.scala](src/col/vct/col/typerules/CoercionUtils.scala) accepts the
matching `TVector` to `TOpenCLVector` coercion when sizes and element types agree.

---

## Implementation choices

| Choice | Why |
|---|---|
| Keep generated sizeof helpers in the AST and C output | Their specification uses must resolve during verification of the generated file. |
| Keep `to-method` outside the default spin pool | Include it explicitly in `ROBUSTNESS_TRANSFORM` / `TRANSFORM` to use it in mixed mode. |

---

## Not implementation

| Path | What it is |
|---|---|
| `robustness-benchmarks/` | Organized inputs, mutants, logs, and result tables; it is evidence, not implementation. |
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

The dump-only command uses an already-built launcher at
`out/vercors/main/runScript.dest/vercors`. Build it with
`./mill -j 0 vercors.main.runScript` when the implementation changes.

---

## If you need to change something later

| You want to | Open |
|---|---|
| Add a transform that does not exist yet | [`robustness.md`](robustness.md) |
| Change which statements a rewrite may touch | The matching `*Sites.scala` |
| Change repeat / spin / seed | `Robustness.scala` and `Options.scala` |
| Change the dump path or C syntax of the mode | `Robustness.scala` `Output(...)` |
| Inspect generated sizeof declarations | The files in the generated sizeof helpers section |
| Fix invalid C in a dump | The pretty-print files in that section |
| Change a folder run | `util/robustness/config.env`, then the script |
| Inspect or rerun the organized benchmarks | `robustness-benchmarks/benchmark-overview.md` |
