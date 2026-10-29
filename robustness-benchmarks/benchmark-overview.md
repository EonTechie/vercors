# Robustness benchmarks

This directory contains the robustness transformations, benchmark inputs,
commands, transformed programs, and verification logs added to the VerCors fork.

The first experiments and their recheck are stored side by side:

```text
robustness-benchmarks/
  pilot/                         # seven-program pilot experiments
  initial-run/
    earlier-four-transformations/
    to-method/
  latest-recheck/
    earlier-four-transformations/
    to-method/
```

`initial-run/` preserves the earlier inputs and results; `latest-recheck/` contains
the repeat with the updated code. The pilot's repeated and mixed modes were not
part of this recheck.

| Work | Scope | Current status |
|---|---|---|
| [Four established transformations](initial-run/earlier-four-transformations/broader-benchmark.md) | Evaluate add-if-zero, add-if-one, for-to-while, and deepen-while separately on the shared 34-program broader input set | Latest recheck: 347 ROBUST for add-if-zero, 347 for add-if-one, and 9 for for-to-while; deepen-while has no applicable sites in this set |
| [to-method](latest-recheck/to-method/to_method_experiment_results_summary.md) | Implement and evaluate the fifth transformation | Latest recheck: 19 programs, 100 applicable sites, 100 ROBUST results |

For the four established transformations, the
[initial summary](initial-run/earlier-four-transformations/four-transformation-results-summary.md)
and [recheck summary](latest-recheck/earlier-four-transformations/four-transformation-results-summary.md)
link each mutant to its input, output and logs. Both distinguish verification
failures under review, excluded attempts and inputs with no applicable site.

For to-method, [to_method_selected_benchmark.md](initial-run/to-method/to_method_selected_benchmark.md)
records the reviewed inventory, selected inputs and excluded files with reasons.
[to_method_experiment_results_summary.md](latest-recheck/to-method/to_method_experiment_results_summary.md)
links each of the 100 inputs, outputs and verification logs and identifies the
helper's verification strategy.

## Latest recheck records

The latest records are under
[latest-recheck/](latest-recheck/benchmark-recheck.md):

- [Four transformations: recheck results](latest-recheck/earlier-four-transformations/four-transformation-results-summary.md): per-site tables, transformed C files, transformation logs, and verification logs.
- [Consolidated to-method recheck](latest-recheck/to-method/to-method-recheck.md): all 100 results, input programs, output files, logs, and the source-run record for the 79 initial results and 21 continuation results.

All previously recorded ROBUST results in these broader evaluations passed
again. The four established transformations now have 703 recorded ROBUST
results in total; to-method has 100. These counts do not include the separate
pilot tests of repeated and mixed modes, which were not rerun in this recheck.

The other records for the four established transformations remain under review.
In particular, eight iteration-contract errors in the for-to-while table are
labelled NOT_ROBUST by the runner but are annotation-processing errors, not
valid NOT_ROBUST results. The raw tables are preserved; their failure labels
should not be treated as final benchmark classifications.

Earlier results remain under `initial-run/` as historical records;
their output files and counts need not
match this recheck.
