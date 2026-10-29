# Latest recheck

This is the repeat of the experiments in [initial-run/](../initial-run/initial-experiments.md), using
the updated implementation. Both directories contain `earlier-four-transformations/`
and `to-method/`. Original execution metadata is preserved in [metadata/](metadata/).

## Consolidated to-method results

Use [the consolidated record](to-method/to-method-recheck.md)
for the completed to-method recheck: 19 programs, 100 distinct sites, 100
recorded ROBUST results. It combines 79 results from the original run and 21
from its completed continuation; it is not a new execution.

The four auxiliary attempts are preserved under `archive/`. The interrupted
original run metadata is retained unchanged. The results of the other four
transformations have not been changed or reclassified by this consolidation.

## Four-transformations recheck results

The repeated runs of add-if-zero, add-if-one, for-to-while and deepen-while are in
[earlier-four-transformations/](earlier-four-transformations/).
The [per-site results summary](earlier-four-transformations/four-transformation-results-summary.md)
links each input, original-verification log, output, transformation log and
output-verification log. It separates verification failures under review from
excluded attempts and inputs with no applicable site.
The shared inputs and earlier results remain in
[initial-run/earlier-four-transformations/](../initial-run/earlier-four-transformations/broader-benchmark.md).

The result directories were moved without changing the output programs, result
tables or logs. Paths inside the original logs and run settings still use the
names from the time of execution.

## Running validation

This directory reruns all five transformations without changing the recorded
benchmark results.

- The four established transformations use the 34 inputs in
  `../initial-run/earlier-four-transformations/inputs/`.
- `to-method` uses the 19 inputs in `../initial-run/to-method/inputs/`.
- Every transformation runs in `preferred` mode with `sites=all` and
  `repeat=1`; spin, mixed, multiple, and chain modes are not used.
- Original verification, transformation, and second verification are recorded
  in a new timestamped directory under `runs/`.
- Each VerCors verification has a 60-minute internal limit. Every VerCors
  process has a 65-minute operating-system limit. The launch command limits
  the complete run to 72 hours.
- Existing inputs and results are never overwritten.

The run writes `summary.tsv`, `results.tsv`, and `excluded.tsv`. Timeouts,
crashes, transformation failures, frontend failures, and missing sites remain
outside the `ROBUST` and `NOT_ROBUST` result table.
