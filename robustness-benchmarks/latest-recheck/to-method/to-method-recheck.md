# Consolidated to-method recheck

- [Experiment results: all 100 inputs, outputs and log links](to_method_experiment_results_summary.md)

19 programs, 100 distinct expected sites, 100 recorded ROBUST results.
This directory combines two executions; no benchmark was rerun during consolidation.

| Contents | Location |
|---|---|
| The 19 input C programs | [inputs/](inputs/) |
| One result per site | [to-method/verdict.tsv](to-method/verdict.tsv) |
| Per-program totals | [to-method/summary.tsv](to-method/summary.tsv) |
| Transformed C, transformation logs and verification logs | `to-method/<program>/site-<number>.c`, `.transform.log`, `.verify.log` |
| Original verification for each selected program result | `to-method/<program>/original.verify.log` |
| Source run and SHA-256 hashes for each result | [provenance.tsv](provenance.tsv) |

The first 79 rows came from `current-20261028-010058`.
The final 21 rows came from `to-method-remaining-20261028-154730`.
That complete continuation is preserved locally at
`latest-recheck/archive/to-method-remaining-20261028-154730/`.
The local archive is excluded from Git; all 21 selected results and their
artifacts are included in this consolidated directory.
Paths in `provenance.tsv` retain the original layout under `current-code-validation/`.
For the first 79 records, `runs/current-20261028-010058/to-method/` now maps to
`latest-recheck/to-method/`; `archive/` is now under `latest-recheck/`.
Archive paths identify local source records, not published links.

The original 79-row tables and unfinished `pointer_relations` artifacts are
preserved locally in `latest-recheck/archive/before-consolidation-20261028/`.
The historical `run.env`, original-verification directory and run logs retain
their original contents and absolute paths; they describe the original execution,
not the combined record. `provenance.tsv` identifies the source execution for each result.

Checks performed during consolidation:

- All 100 program/site pairs match the benchmark source inventory, with no duplicates.
- The 21 continuation rows have successful original and transformed verification logs.
- All 100 selected results have successful original and transformed verification logs.
- All 300 selected C/transform-log/verify-log files retain their source hashes.
- The continuation's eight input files match the corresponding benchmark inputs.

The [initial experiment summary](../../initial-run/to-method/to_method_experiment_results_summary.md)
provides the earlier outputs and logs for comparison with this recheck.
