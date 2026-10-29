# Initial broader benchmark run

This is the recorded run of `add-if-zero`, `add-if-one`, `for-to-while`, and
`deepen-while`, filtered to the 34 programs selected by
[`SOURCES.tsv`](../../SOURCES.tsv). Four expected-error programs that were in
the original 38-program run are not included here.

The matching original-verification logs are in
[`original-verification/`](../../original-verification/). Every one of the 34
logs contains `Verification completed successfully`.

The [per-site results summary](../../four-transformation-results-summary.md)
links each input, output and log, and separates verification failures under
review from excluded attempts.

The table below retains the labels in the archived TSV files.

| Transformation | Recorded ROBUST | Recorded NOT_ROBUST | Recorded result rows | Excluded records |
|---|---:|---:|---:|---:|
| add-if-zero | 180 | 111 | 291 | 143 |
| add-if-one | 180 | 111 | 291 | 143 |
| for-to-while | 1 | 6 | 7 | 38 |
| deepen-while | 0 | 0 | 0 | 34 |
| **Total** | **361** | **228** | **589** | **358** |

The recorded `NOT_ROBUST` labels are not final benchmark classifications.
Thirty of these rows (15 for each add-if transformation) report invalid
`send`/`recv` placement and are excluded in the per-site summary. The other
198 verification failures remain under review. The archived TSV files are
unchanged.

Each transformation directory contains:

- `results.tsv`: one recorded `ROBUST` or `NOT_ROBUST` label per mutant;
- `excluded.tsv`: tool failures and programs without an applicable site;
- `all-records.tsv`: the complete recorded verdict data;
- `program-summary.tsv`: counts per input program;
- one directory per program containing discovery, transform, C, and
  verification files.

This records the implementation state used for the initial experiment. It was not rerun
after later source changes.
