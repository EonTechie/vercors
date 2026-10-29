# Broader benchmark for the four established transformations

The same 34 verified, non-expected-error C programs are used for add-if-zero, add-if-one, for-to-while, and deepen-while.

Applicability and site discovery are evaluated separately for each transformation.
The earlier seven-program [pilot benchmark](../../pilot/pilot-benchmark.md)
contains the preferred, multiple, and mixed results for that input set.

The copied inputs are in [inputs/](inputs/) and their original VerCors paths are in [SOURCES.tsv](SOURCES.tsv).

[broader-benchmark-selection.md](broader-benchmark-selection.md) records the review of all 88 C examples and why the 34 verified programs form this input set.

The original-verification logs for these 34 programs are in
[original-verification/](original-verification/).

The [initial results summary](four-transformation-results-summary.md) has one row
per mutant, linking the input, original-verification log, output, transformation
log and output-verification log. Excluded attempts and inputs with no applicable
site are listed separately. Verification failures remain under review.

The [archived aggregate table](results/run-20261026-015207/initial-transformation-results.md)
preserves the earlier counts and explains the recorded TSV files.
The [recheck summary](../../latest-recheck/earlier-four-transformations/four-transformation-results-summary.md)
uses the same format for the repeat with updated code.

## Run

From the repository root:

    ./robustness-benchmarks/initial-run/earlier-four-transformations/run.sh RUN_ID
