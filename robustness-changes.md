# What changed after the VerCors fork

This document explains the robustness implementation and changes to shared
VerCors code on `robustness-post-resolution`. Source links point to the current
files. [robustness-files.md](robustness-files.md) maps their responsibilities;
[robustness.md](robustness.md) explains how to add another transformation.

## Current status

- VerCors has five robustness transforms: `add-if-zero`, `add-if-one`,
  `for-to-while`, `deepen-while`, and `to-method`.
- `to-method` records 100 ROBUST results across 19 programs and 100 sites in both
  the initial experiment and latest recheck. The
  [latest results summary](robustness-benchmarks/latest-recheck/to-method/to_method_experiment_results_summary.md)
  links every input, output and log.
- The four earlier transformations share
  [34 selected inputs](robustness-benchmarks/initial-run/earlier-four-transformations/broader-benchmark-selection.md).
  Their latest ROBUST counts are 347 for add-if-zero, 347 for add-if-one and
  9 for for-to-while, compared with 180, 180 and 1 in the initial run.
  Deepen-while has no applicable sites in this set.
- Remaining failure classifications are under review, including eight
  for-to-while rows whose NOT_ROBUST labels actually describe iteration-contract
  processing errors. See the [benchmark overview](robustness-benchmarks/benchmark-overview.md)
  for the records and classification caveat. The repeated and mixed pilot
  experiments were not part of this recheck.

## Current to-method implementation

The source paths for this implementation are mapped in
[robustness-files.md](robustness-files.md#to-method-source-map).

### Selection, parameters and calls

[ToMethodSites.scala](src/rewrite/vct/rewrite/ToMethodSites.scala), in `collect`,
`selectable` and `forbidden`, selects a nonempty block below a function body. It
rejects blocks containing declarations, scopes with locals, nested loops,
`switch`, nonlocal control transfers, or `send`/`recv`. It can select a child block
inside a loop or branch. Verification helper functions (`reach_error`, `abort`,
`assume_abort_if_not`, `__VERIFIER_*`) are skipped by function name. There are no
benchmark-filename tests. This is a restricted structural eligibility rule, not
a proof that every eligible block can be handled by every subsequent stage.

`outwardVars` collects references to surrounding locals while excluding names
bound inside `Binder` and `ScopedExpr` nodes, including quantified and `\let`
expressions. `outwardHeapVars` separately collects address-taken locals represented
as `HeapLocal` nodes. These are input analyses, not transformations of the example
source files.

[RobustnessToMethod.scala](src/rewrite/vct/rewrite/RobustnessToMethod.scala), in
`extract`, captures the selected block's variables and any variables needed by
its enclosing loop guard. Read-only captures are passed by value. Written captures
normally use pointer parameters and dereferences, so the call updates the caller's
local. `decayToPointer` converts supported array types to pointer parameter types;
address-taken local storage is passed separately through pointer parameters.

There is also a return-value path: `useReturnValue` selects it when exactly one
captured local is assigned and that local or another capture has a supported
pointer/array type. The caller assigns the helper's return value to that local.
This helper is always inline. The original block is replaced by a call or call
assignment, retaining surrounding braces through
[RobustnessStatementWrap.scala](src/rewrite/vct/rewrite/RobustnessStatementWrap.scala).

### Contracts and inline expansion

The modular path is deliberately narrow. The helper body must consist only of
assignments through captured-local pointer parameters, with each target assigned
once. `functionalContractSafe` accepts a restricted expression set and rejects a
later right-hand side that reads a previously assigned target. The helper also
must not have pointer values captured by value (`ptrValueParams`). For this path,
the code generates permission context and `ensures` clauses using `\old`, and
uses the captured loop guard as `requires` when present. Such a helper can be
verified separately from its callers.

The checks in `extract`, `asPtrAssign`, `functionalContractSafe` and
`oldifyParamReads` make these limits concrete:

1. After unwrapping blocks and scopes, the body must contain at least one
   statement, and every statement must be an assignment recognized by
   `asPtrAssign`. An `if`, loop, assertion or standalone call in the body does
   not meet this condition.
2. Each assignment must write through a parameter representing a captured
   local passed by reference. Assignments to other targets do not qualify.
3. Each target parameter may be assigned only once in the block.
4. An assignment's right-hand side must not read a target assigned earlier
   in the block. Reading the target's own value before its first assignment
   is allowed.
5. Reads through these by-reference parameters must be direct reads or occur
   inside supported `+`, `-` or `*` expressions, including their ambiguous AST
   variants. The generator translates those reads to `\old` values. An expression
   subtree that contains no such reads is also accepted by this check and is
   left unchanged; this is not a blanket ban on other operators.

For separate helper verification, `ptrValueParams` must also be empty: captured
pointer/array values and captured heap-local storage select `inline`. The
return-value extraction path described above always selects `inline` as well.
These restrictions choose how an extracted helper is verified; they are not
the rules for selecting a transformation site.

The following examples illustrate the source-code checks, not additional
benchmark executions. Assume `x` and `y` are surrounding integer locals and
the block has no other captures or statements:

| Block | Contract-generation decision |
|---|---|
| `x = x + 1; y = y * 2;` | Meets these checks: each target is assigned once, and neither assignment reads a target modified earlier. |
| `x = 1; x = 2;` | Uses `inline`: the same target is assigned twice. |
| `x = 1; y = x + 2;` | Uses `inline`: the second assignment reads the newly assigned `x`, which cannot be replaced by its entry value. |
| `x = x / 2;` | Uses `inline`: division containing a read of the by-reference `x` is not supported by `oldifyParamReads`. |
| `x = y / 2;` | Meets these checks: read-only `y` is passed by value, so this expression contains no by-reference parameter read requiring conversion to `\old`. |

Otherwise the supported extraction uses `inline`. This includes sequentially
dependent assignments outside that contract generator's supported cases and
pointer/array captures. It does **not** mean that a correct contract is impossible;
it means this implementation does not synthesize a modular contract for that case.
[ProcedureImpl.scala](src/col/vct/col/ast/declaration/global/ProcedureImpl.scala)
prints the marker and procedure annotations. In the ordinary verification
pipeline, the existing
[InlineApplicables.scala](src/rewrite/vct/rewrite/InlineApplicables.scala)
substitutes arguments and expands the helper body at the call. The resulting
body is then verified in the caller's context. Inline expansion is not an
assumption that the body is correct, but these cases do not test independent
verification of generated helper contracts.

The current 100-output record contains 98 inline helpers and two helpers with
separate contracts: `3_while_multip` site 0 and `pallas_c_loop_unused` site 0.
The [results table](robustness-benchmarks/latest-recheck/to-method/to_method_experiment_results_summary.md)
labels these cases and links their actual C definitions.

### Loop annotations and C output

`frameLoop` adds pointer permissions for captured locals passed by reference to a
`LoopInvariant`, while retaining the original invariant and decreases clauses.
Where needed, a `for` with a loop invariant is represented as initialization plus
`while`, with its update retained. Iteration-contract and `send`/`recv` cases keep
the `for` form; `keepAsFor` runs within `sendDecls.scope`. This preserves declaration
resolution for surrounding `send`/`recv` annotations; it does not move those
annotations into helpers (they are forbidden inside the selected block).

The shared C writer and C-to-COL conversion were also extended for constructs
encountered in these inputs. They preserve non-null allocation using the
VerCors-recognized `vercorsMallocNonNull` representation, initialize non-null
heap-local storage, print `unique<n>` as a C specification annotation, print
Euclidean arithmetic nodes as `\euclidean_div`/`\euclidean_mod`, and print
procedure contracts. Mutable C globals are printed without the internal extra
pointer layer used to represent their storage. These are shared printing and
re-reading rules, not per-file edits or a to-method-only verifier. The exact
[source files are listed here](robustness-files.md#to-method-source-map).

The resulting files target VerCors's annotated C front end. The generated
allocation helper is interpreted by that front end; these experiments are not a
claim about compiling and running the generated files as standalone C executables.

The emitted file is a printing of the resolved COL program, not a textual copy
of the original with only a function call inserted. For example, the existing
`LangCToCol` array-initialization handling represents initializer values with
`Assume` nodes; these appear as `assume` annotations in the C output of
`ptr_comparisons` and `tagged_struct`. They correspond to the original array
initializers, not assumptions added by `RobustnessToMethod` to force a verdict.
Ordinary comments and includes also disappear during parsing, so a raw count of
the word `assert` is not a reliable check for removed assertion statements.

The following concrete examples connect the rules above to the current artifacts.
The linked C files also expose the contracts and annotations for inspection.

| Implemented behavior | Input | Current transformed file |
|---|---|---|
| Pointer parameters, `\old` postconditions and added loop permissions; separate helper verification | [3_while_multip.c](robustness-benchmarks/initial-run/to-method/inputs/3_while_multip.c) | [site 0](robustness-benchmarks/latest-recheck/to-method/to-method/3_while_multip/site-0.c) |
| A single changed value returned by an inline helper | [4_big.c](robustness-benchmarks/initial-run/to-method/inputs/4_big.c) | [site 0](robustness-benchmarks/latest-recheck/to-method/to-method/4_big/site-0.c) |
| Quantified assertion moved with its bound `j` kept inside the quantifier, not passed as a parameter | [vector_add.c](robustness-benchmarks/initial-run/to-method/inputs/vector_add.c) | [site 1](robustness-benchmarks/latest-recheck/to-method/to-method/vector_add/site-1.c) |
| Extracted assignment with surrounding iteration contracts and `send`/`recv` retained | [basic-examples.c](robustness-benchmarks/initial-run/to-method/inputs/basic-examples.c) | [site 4](robustness-benchmarks/latest-recheck/to-method/to-method/basic-examples/site-4.c) |
| Address-taken struct local passed through a pointer, with non-null storage in C output | [pointer_casts.c](robustness-benchmarks/initial-run/to-method/inputs/pointer_casts.c) | [site 1](robustness-benchmarks/latest-recheck/to-method/to-method/pointer_casts/site-1.c) |
| Uniqueness qualifiers retained in parameter annotations | [arrays.c](robustness-benchmarks/initial-run/to-method/inputs/arrays.c) | [site 0](robustness-benchmarks/latest-recheck/to-method/to-method/arrays/site-0.c) |
| Euclidean arithmetic printed in VerCors annotation syntax | [mod_div.c](robustness-benchmarks/initial-run/to-method/inputs/mod_div.c) | [site 0](robustness-benchmarks/latest-recheck/to-method/to-method/mod_div/site-0.c) |

### Pipeline, inspiration and scope of the evidence

[Robustness.scala](src/main/vct/main/modes/Robustness.scala) parses and resolves
the input, applies the selected AST rewrite and writes C. It does not run the
verification backend in robustness mode. The
[benchmark driver](util/robustness/run-benchmark.sh) separately verifies the
original and the emitted C program through VerCors's ordinary pipeline.

The extraction idea is related to the external SemTransform project:
[`to_method` in `strong.py`](https://github.com/FlorianDyck/semtransforms/blob/master/semtransforms/transformations/strong.py#L82)
moves a compound statement into a function and passes surrounding locals through
pointers. Our implementation uses VerCors AST nodes, annotations, permissions and
inline handling; it is not that Python transformer or a claim that its formal
results prove this implementation correct.

The [selection report](robustness-benchmarks/initial-run/to-method/to_method_selected_benchmark.md)
records the 88-file VerCors inventory and the separate seven-program pilot set;
19 programs contribute the selected 100 sites. All 100 recorded transformations
verify in the latest recheck. This evidence supports those concrete cases,
not universal applicability, full contract inference or a proof of semantic
preservation for arbitrary C programs.

### Potential issues pending testing

Code review identified three possible correctness issues that are not exercised
by the 19 selected programs. These have not yet been reproduced in dedicated
tests or corrected; they are distinct from the supported-site and
contract-generation restrictions described above.

- **Nested assignments**, such as `x = (y = 1)`: the assigned-variable analysis
  in [RobustnessToMethod.scala](src/rewrite/vct/rewrite/RobustnessToMethod.scala)
  may record the outer write to `x` without recording the inner write to `y`,
  so an update to a captured local could be lost.
- **`continue` in an enclosing `for` loop**, outside the extracted block:
  converting that loop to `while` in
  [RobustnessToMethod.scala](src/rewrite/vct/rewrite/RobustnessToMethod.scala)
  could let `continue` skip the update moved to the end of the loop body.
- **Postfix increments whose value is used**, such as `x = y++`: the shared
  C output in
  [PostAssignExpressionImpl.scala](src/col/vct/col/ast/expr/sideeffect/PostAssignExpressionImpl.scala)
  may print an assignment that returns the updated value rather than the
  original value required by the postfix expression.

Each case needs a dedicated test before a correction is confirmed, followed by
a rerun of the existing 19-program, 100-site benchmark to check for regressions.
The recorded 100 ROBUST results do not test these cases or establish their
correctness.

## Added robustness implementation

These files implement the robustness mode, site selection and experiment driver.

| Source | Responsibility |
|---|---|
| [Robustness.scala](src/main/vct/main/modes/Robustness.scala) | The `--robustness` mode. Parse, resolve, run the builders from `ROBUSTNESS_TRANSFORM` / spin, print C to `robustness-transformed.c`. Does not run Silicon. |
| [AddIfZero.scala](src/rewrite/vct/rewrite/AddIfZero.scala) | Rewrite `add-if-zero`: wrap one collected statement as `if (0) { assert false } else { S }`. |
| [AddIfOne.scala](src/rewrite/vct/rewrite/AddIfOne.scala) | Rewrite `add-if-one`: same collector as AddIfZero; wrap as `if (1) { S } else { assert false }`. |
| [RobustnessForLoopToWhileLoop.scala](src/rewrite/vct/rewrite/RobustnessForLoopToWhileLoop.scala) | Rewrite `for-to-while`: convert one collected `for` by calling `ForLoopToWhileLoop.rewriteLoop`. |
| [RobustnessDeepenWhile.scala](src/rewrite/vct/rewrite/RobustnessDeepenWhile.scala) | Rewrite `deepen-while`: nest one collected `while` as outer `while (c)` plus inner `while (nondet() && c)`. |
| [RobustnessToMethod.scala](src/rewrite/vct/rewrite/RobustnessToMethod.scala) | Extracts a selected block using the parameter, contract and inline rules described above. |
| [CStatementSites.scala](src/rewrite/vct/rewrite/CStatementSites.scala) | Collector for add-if-zero and add-if-one: wrappable C statements. |
| [ForLoopToWhileLoopSites.scala](src/rewrite/vct/rewrite/ForLoopToWhileLoopSites.scala) | Collector for for-to-while: `for` loops without `continue`. |
| [DeepenWhileSites.scala](src/rewrite/vct/rewrite/DeepenWhileSites.scala) | Collects while loops with a loop invariant, excluding generated inner loops, unsupported control flow and conditions with assignments or calls. |
| [ToMethodSites.scala](src/rewrite/vct/rewrite/ToMethodSites.scala) | Collector for to-method: blocks that can be extracted. |
| [RobustnessSiteSelection.scala](src/rewrite/vct/rewrite/RobustnessSiteSelection.scala) | Picks one site index per application from `*_SITE` or 0; spin uses `forceNext`. |
| [RobustnessSpin.scala](src/rewrite/vct/rewrite/RobustnessSpin.scala) | One mixed step: uniform choice of kind among remaining kinds with sites, then a uniform site index. |
| [RobustnessStatementWrap.scala](src/rewrite/vct/rewrite/RobustnessStatementWrap.scala) | Keeps `{ }` when a Block/Scope body is replaced by a node the C printer would not brace. |
| [run-benchmark.sh](util/robustness/run-benchmark.sh) | Runs original verification, transformation and output verification; records logs and TSV verdicts. Failure labels require the review described below. |
| [config.env](util/robustness/config.env) | Provides the benchmark driver settings: mode, transformation, inputs, outputs, verification and site selection. |

## Changes to existing VerCors components

### Mode and command-line integration

[Mode.scala](src/main/vct/options/types/Mode.scala) adds `Mode.Robustness`;
[Main.scala](src/main/vct/main/Main.scala) dispatches it to `Robustness.runOptions`.
[Options.scala](src/main/vct/options/Options.scala) adds `--robustness`,
`--robustness-repeat`, `--robustness-spin` and `--robustness-seed`.
The transformation name is read from `ROBUSTNESS_TRANSFORM`.
The robustness rewriters run in this mode rather than the ordinary verification
pass list in [Transformation.scala](src/main/vct/main/stages/Transformation.scala).

### Shared for-to-while conversion

The existing loop conversion was extracted into `rewriteLoop` in
[ForLoopToWhileLoop.scala](src/rewrite/vct/rewrite/ForLoopToWhileLoop.scala).
The ordinary verification pass calls it for loops in its traversal;
[RobustnessForLoopToWhileLoop.scala](src/rewrite/vct/rewrite/RobustnessForLoopToWhileLoop.scala)
calls it only for the selected loop. Initialization is placed before the while
loop and the update after its body. The robustness collector excludes loops
with a `continue` targeting the converted loop.

### Generated sizeof functions and other declarations

[LangCToCol.scala](src/rewrite/vct/rewrite/lang/LangCToCol.scala) tags generated
sizeof functions with `GeneratedSizeOfHelper`, defined in
[Origin.scala](src/col/vct/col/origin/Origin.scala).
[FunctionImpl.scala](src/col/vct/col/ast/declaration/global/FunctionImpl.scala)
retains the `isGeneratedSizeOfHelper` predicate. The current
[ProgramImpl.scala](src/col/vct/col/ast/node/ProgramImpl.scala) does not filter
these functions: it prints all declarations, including sizeof specification
functions needed when annotations are read again. It also adds missing
declarations for `vercorsMalloc`, `vercorsMallocNonNull` and `vercorsFree`.

### Shared C output and re-reading

The shared printers provide C layouts for resolved COL constructs, allowing the
emitted program to be read by VerCors again. The source links identify the
syntax-specific cases; they do not imply support for every possible C input.

| Area | Current behavior and source |
|---|---|
| Identifiers | [Ctx.scala](src/col/vct/col/print/Ctx.scala), `cIdentifier`, replaces punctuation with underscores and prefixes empty or digit-leading names when printing C-family syntax. |
| Arrays and pointers | [TArrayImpl.scala](src/col/vct/col/ast/type/TArrayImpl.scala), [TPointerArrayImpl.scala](src/col/vct/col/ast/type/TPointerArrayImpl.scala), [TConstPointerArrayImpl.scala](src/col/vct/col/ast/type/TConstPointerArrayImpl.scala), [TNonNullPointerArrayImpl.scala](src/col/vct/col/ast/type/TNonNullPointerArrayImpl.scala) and [TNonNullConstPointerArrayImpl.scala](src/col/vct/col/ast/type/TNonNullConstPointerArrayImpl.scala) supply C pointer declarators for resolved array types. |
| Booleans and null | [TBoolImpl.scala](src/col/vct/col/ast/type/TBoolImpl.scala) prints `_Bool` for C; [BooleanValueImpl.scala](src/col/vct/col/ast/expr/literal/constant/BooleanValueImpl.scala) prints executable C booleans as `1`/`0` while specifications retain `true`/`false`; [NullImpl.scala](src/col/vct/col/ast/expr/literal/constant/NullImpl.scala) prints `NULL` for C-family syntax. |
| Structs | [TClassImpl.scala](src/col/vct/col/ast/type/TClassImpl.scala) and [ClassImpl.scala](src/col/vct/col/ast/declaration/global/ClassImpl.scala) print resolved by-value classes as C struct types and declarations. |
| Procedures | [ProcedureImpl.scala](src/col/vct/col/ast/declaration/global/ProcedureImpl.scala) includes contracts and the `inline` marker in C output. |
| Loops | [LoopImpl.scala](src/col/vct/col/ast/statement/composite/LoopImpl.scala) handles resolved initializer statements in for headers and uses the statement shape to print body braces. |
| Allocation, heap locals, arithmetic, uniqueness and globals | The [to-method source map](robustness-files.md#to-method-source-map) identifies the shared printers and C-to-COL handling used by the examples described above. |

## Benchmark driver and result interpretation

[run-benchmark.sh](util/robustness/run-benchmark.sh) provides `preferred`,
`multiple`, `chain`, `mixed` and `verify-only` modes. In `preferred` mode, each
site is transformed from the original program in a separate invocation.
The driver verifies originals when `VERIFY_ORIGINALS=1` and outputs when
`VERIFY_MUTANTS=1`. It can reuse existing original-verification logs unless
`FORCE_ORIGINAL=1`; a fresh output directory avoids reusing a previous run.

`candidate_count`, `transform_status` and `verify_status` read the logs.
`verdict` distinguishes missing sites, transformation failures, skipped original
verification, success, and recognized crash, timeout, parse and front-end failures.
Its remaining unsuccessful cases are labelled NOT_ROBUST. This is an operational
classification that still needs log review: the iteration-contract errors noted
in the current status section fall through to that label.

A valid robustness comparison needs a verified original and a transformation
that preserves program behavior and annotation obligations. Failures caused by
invalid output, tool errors or missing sites are not evidence that a valid
transformation broke the proof. The [benchmark overview](robustness-benchmarks/benchmark-overview.md)
links the initial and repeated runs, and [robustness.md](robustness.md)
explains how to integrate another transformation with this driver.

## Retired prototype components

The earlier text-based transformation stages `CSourceRewriter.scala`,
`CSourceTransformation.scala` and `CInsertionSites.scala` are absent from the
current implementation. Statement wrapping and block extraction now operate on
the resolved AST through the sources listed above.

The prototype flags `--dev-add-if-zero`, `--dev-add-if-one` and `--trace-tree-in`
and the `outputTree` option field are absent from the current option definitions.
The active interface is `--robustness` with the five transformation names in
`parseTransform`. Their underscore spellings are also accepted; the former
`if0error`, `add_if1` and `for2while` aliases are not.
