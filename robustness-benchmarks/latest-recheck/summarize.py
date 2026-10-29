#!/usr/bin/env python3
import csv
import sys
from collections import Counter
from pathlib import Path


root = Path(sys.argv[1])
established_directory = root / "earlier-four-transformations"
if not established_directory.exists():
    established_directory = root / "four-transformations-results"
if not established_directory.exists():
    established_directory = root / "established-transformations"
groups = [
    ("established-transformations", established_directory, [
        "add-if-zero", "add-if-one", "for-to-while", "deepen-while"
    ]),
    ("to-method", root / "to-method", ["to-method"]),
]

summary_rows = []
result_rows = []
excluded_rows = []

for group, directory, transforms in groups:
    for transform in transforms:
        verdict_path = directory / transform / "verdict.tsv"
        if not verdict_path.exists():
            summary_rows.append({
                "group": group,
                "transform": transform,
                "ROBUST": 0,
                "NOT_ROBUST": 0,
                "excluded": 0,
                "status": "MISSING_VERDICT_TABLE",
            })
            continue

        counts = Counter()
        with verdict_path.open(newline="") as handle:
            for row in csv.DictReader(handle, delimiter="\t"):
                verdict = row["verdict"]
                counts[verdict] += 1
                record = {
                    "group": group,
                    "transform": transform,
                    "program": row["program"],
                    "site": row["site"],
                    "verdict": verdict,
                }
                if verdict in {"ROBUST", "NOT_ROBUST"}:
                    result_rows.append(record)
                else:
                    excluded_rows.append(record)

        summary_rows.append({
            "group": group,
            "transform": transform,
            "ROBUST": counts["ROBUST"],
            "NOT_ROBUST": counts["NOT_ROBUST"],
            "excluded": sum(
                count for verdict, count in counts.items()
                if verdict not in {"ROBUST", "NOT_ROBUST"}
            ),
            "status": "COMPLETE",
        })


def write_table(path, fieldnames, rows):
    with path.open("w", newline="") as handle:
        writer = csv.DictWriter(
            handle, fieldnames=fieldnames, delimiter="\t", lineterminator="\n"
        )
        writer.writeheader()
        writer.writerows(rows)


write_table(
    root / "summary.tsv",
    ["group", "transform", "ROBUST", "NOT_ROBUST", "excluded", "status"],
    summary_rows,
)
write_table(
    root / "results.tsv",
    ["group", "transform", "program", "site", "verdict"],
    result_rows,
)
write_table(
    root / "excluded.tsv",
    ["group", "transform", "program", "site", "verdict"],
    excluded_rows,
)
