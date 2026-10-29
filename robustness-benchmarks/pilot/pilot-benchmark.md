# Seven-program pilot benchmark for the four established transformations

This directory is the self-contained pilot benchmark used before the broader benchmark.

## Inputs

The seven original C programs are in [inputs/](inputs/). Their original verification logs are in [original-verification/](original-verification/).

## Preferred mode results

| Transformation | ROBUST | Status |
|---|---:|---|
| add-if-zero | 24 | complete |
| add-if-one | 24 | complete |
| for-to-while | 4 | complete; four programs have no applicable loop |
| deepen-while | 0 | one applicable site exists, but its second verification is not recorded |

The machine-readable table is [results/preferred/results.tsv](results/preferred/results.tsv).

## Other driver modes

| Mode | Recorded ROBUST outputs | Meaning |
|---|---:|---|
| multiple | 34 | repeat 2 and repeat 5 for transforms with an applicable site |
| mixed | 7 | seed 42, repeat 5 |
| chain | 0 | implemented, but no stored completed pilot run |
| verify-only | 7 originals | baseline verification, not a mutant result |

## Per-site preferred results

| Transformation | Program | Site | Input | Transformed | Transform log | Verify log | Result |
|---|---|---:|---|---|---|---|---|
| add-if-zero | 1_absolute | 0 | [input](inputs/1_absolute.c) | [after](results/preferred/add-if-zero/1_absolute/site-000/transformed.c) | [log](results/preferred/add-if-zero/1_absolute/site-000/transform.log) | [verify](results/preferred/add-if-zero/1_absolute/site-000/verify.log) | ROBUST |
| add-if-zero | 2_swap | 0 | [input](inputs/2_swap.c) | [after](results/preferred/add-if-zero/2_swap/site-000/transformed.c) | [log](results/preferred/add-if-zero/2_swap/site-000/transform.log) | [verify](results/preferred/add-if-zero/2_swap/site-000/verify.log) | ROBUST |
| add-if-zero | 2_swap | 1 | [input](inputs/2_swap.c) | [after](results/preferred/add-if-zero/2_swap/site-001/transformed.c) | [log](results/preferred/add-if-zero/2_swap/site-001/transform.log) | [verify](results/preferred/add-if-zero/2_swap/site-001/verify.log) | ROBUST |
| add-if-zero | 3_mult_add | 0 | [input](inputs/3_mult_add.c) | [after](results/preferred/add-if-zero/3_mult_add/site-000/transformed.c) | [log](results/preferred/add-if-zero/3_mult_add/site-000/transform.log) | [verify](results/preferred/add-if-zero/3_mult_add/site-000/verify.log) | ROBUST |
| add-if-zero | 3_while_multip | 0 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-zero/3_while_multip/site-000/transformed.c) | [log](results/preferred/add-if-zero/3_while_multip/site-000/transform.log) | [verify](results/preferred/add-if-zero/3_while_multip/site-000/verify.log) | ROBUST |
| add-if-zero | 3_while_multip | 1 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-zero/3_while_multip/site-001/transformed.c) | [log](results/preferred/add-if-zero/3_while_multip/site-001/transform.log) | [verify](results/preferred/add-if-zero/3_while_multip/site-001/verify.log) | ROBUST |
| add-if-zero | 3_while_multip | 2 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-zero/3_while_multip/site-002/transformed.c) | [log](results/preferred/add-if-zero/3_while_multip/site-002/transform.log) | [verify](results/preferred/add-if-zero/3_while_multip/site-002/verify.log) | ROBUST |
| add-if-zero | 3_while_multip | 3 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-zero/3_while_multip/site-003/transformed.c) | [log](results/preferred/add-if-zero/3_while_multip/site-003/transform.log) | [verify](results/preferred/add-if-zero/3_while_multip/site-003/verify.log) | ROBUST |
| add-if-zero | 4_big | 0 | [input](inputs/4_big.c) | [after](results/preferred/add-if-zero/4_big/site-000/transformed.c) | [log](results/preferred/add-if-zero/4_big/site-000/transform.log) | [verify](results/preferred/add-if-zero/4_big/site-000/verify.log) | ROBUST |
| add-if-zero | 4_big | 1 | [input](inputs/4_big.c) | [after](results/preferred/add-if-zero/4_big/site-001/transformed.c) | [log](results/preferred/add-if-zero/4_big/site-001/transform.log) | [verify](results/preferred/add-if-zero/4_big/site-001/verify.log) | ROBUST |
| add-if-zero | 4_big | 2 | [input](inputs/4_big.c) | [after](results/preferred/add-if-zero/4_big/site-002/transformed.c) | [log](results/preferred/add-if-zero/4_big/site-002/transform.log) | [verify](results/preferred/add-if-zero/4_big/site-002/verify.log) | ROBUST |
| add-if-zero | 4_big | 3 | [input](inputs/4_big.c) | [after](results/preferred/add-if-zero/4_big/site-003/transformed.c) | [log](results/preferred/add-if-zero/4_big/site-003/transform.log) | [verify](results/preferred/add-if-zero/4_big/site-003/verify.log) | ROBUST |
| add-if-zero | 4_big | 4 | [input](inputs/4_big.c) | [after](results/preferred/add-if-zero/4_big/site-004/transformed.c) | [log](results/preferred/add-if-zero/4_big/site-004/transform.log) | [verify](results/preferred/add-if-zero/4_big/site-004/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 0 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-000/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-000/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-000/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 1 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-001/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-001/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-001/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 2 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-002/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-002/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-002/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 3 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-003/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-003/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-003/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 4 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-004/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-004/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-004/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 5 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-005/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-005/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-005/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 6 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-006/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-006/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-006/verify.log) | ROBUST |
| add-if-zero | 5_bubbleSort | 7 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-zero/5_bubbleSort/site-007/transformed.c) | [log](results/preferred/add-if-zero/5_bubbleSort/site-007/transform.log) | [verify](results/preferred/add-if-zero/5_bubbleSort/site-007/verify.log) | ROBUST |
| add-if-zero | 6_pure | 0 | [input](inputs/6_pure.c) | [after](results/preferred/add-if-zero/6_pure/site-000/transformed.c) | [log](results/preferred/add-if-zero/6_pure/site-000/transform.log) | [verify](results/preferred/add-if-zero/6_pure/site-000/verify.log) | ROBUST |
| add-if-zero | 6_pure | 1 | [input](inputs/6_pure.c) | [after](results/preferred/add-if-zero/6_pure/site-001/transformed.c) | [log](results/preferred/add-if-zero/6_pure/site-001/transform.log) | [verify](results/preferred/add-if-zero/6_pure/site-001/verify.log) | ROBUST |
| add-if-zero | 6_pure | 2 | [input](inputs/6_pure.c) | [after](results/preferred/add-if-zero/6_pure/site-002/transformed.c) | [log](results/preferred/add-if-zero/6_pure/site-002/transform.log) | [verify](results/preferred/add-if-zero/6_pure/site-002/verify.log) | ROBUST |
| add-if-one | 1_absolute | 0 | [input](inputs/1_absolute.c) | [after](results/preferred/add-if-one/1_absolute/site-000/transformed.c) | [log](results/preferred/add-if-one/1_absolute/site-000/transform.log) | [verify](results/preferred/add-if-one/1_absolute/site-000/verify.log) | ROBUST |
| add-if-one | 2_swap | 0 | [input](inputs/2_swap.c) | [after](results/preferred/add-if-one/2_swap/site-000/transformed.c) | [log](results/preferred/add-if-one/2_swap/site-000/transform.log) | [verify](results/preferred/add-if-one/2_swap/site-000/verify.log) | ROBUST |
| add-if-one | 2_swap | 1 | [input](inputs/2_swap.c) | [after](results/preferred/add-if-one/2_swap/site-001/transformed.c) | [log](results/preferred/add-if-one/2_swap/site-001/transform.log) | [verify](results/preferred/add-if-one/2_swap/site-001/verify.log) | ROBUST |
| add-if-one | 3_mult_add | 0 | [input](inputs/3_mult_add.c) | [after](results/preferred/add-if-one/3_mult_add/site-000/transformed.c) | [log](results/preferred/add-if-one/3_mult_add/site-000/transform.log) | [verify](results/preferred/add-if-one/3_mult_add/site-000/verify.log) | ROBUST |
| add-if-one | 3_while_multip | 0 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-one/3_while_multip/site-000/transformed.c) | [log](results/preferred/add-if-one/3_while_multip/site-000/transform.log) | [verify](results/preferred/add-if-one/3_while_multip/site-000/verify.log) | ROBUST |
| add-if-one | 3_while_multip | 1 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-one/3_while_multip/site-001/transformed.c) | [log](results/preferred/add-if-one/3_while_multip/site-001/transform.log) | [verify](results/preferred/add-if-one/3_while_multip/site-001/verify.log) | ROBUST |
| add-if-one | 3_while_multip | 2 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-one/3_while_multip/site-002/transformed.c) | [log](results/preferred/add-if-one/3_while_multip/site-002/transform.log) | [verify](results/preferred/add-if-one/3_while_multip/site-002/verify.log) | ROBUST |
| add-if-one | 3_while_multip | 3 | [input](inputs/3_while_multip.c) | [after](results/preferred/add-if-one/3_while_multip/site-003/transformed.c) | [log](results/preferred/add-if-one/3_while_multip/site-003/transform.log) | [verify](results/preferred/add-if-one/3_while_multip/site-003/verify.log) | ROBUST |
| add-if-one | 4_big | 0 | [input](inputs/4_big.c) | [after](results/preferred/add-if-one/4_big/site-000/transformed.c) | [log](results/preferred/add-if-one/4_big/site-000/transform.log) | [verify](results/preferred/add-if-one/4_big/site-000/verify.log) | ROBUST |
| add-if-one | 4_big | 1 | [input](inputs/4_big.c) | [after](results/preferred/add-if-one/4_big/site-001/transformed.c) | [log](results/preferred/add-if-one/4_big/site-001/transform.log) | [verify](results/preferred/add-if-one/4_big/site-001/verify.log) | ROBUST |
| add-if-one | 4_big | 2 | [input](inputs/4_big.c) | [after](results/preferred/add-if-one/4_big/site-002/transformed.c) | [log](results/preferred/add-if-one/4_big/site-002/transform.log) | [verify](results/preferred/add-if-one/4_big/site-002/verify.log) | ROBUST |
| add-if-one | 4_big | 3 | [input](inputs/4_big.c) | [after](results/preferred/add-if-one/4_big/site-003/transformed.c) | [log](results/preferred/add-if-one/4_big/site-003/transform.log) | [verify](results/preferred/add-if-one/4_big/site-003/verify.log) | ROBUST |
| add-if-one | 4_big | 4 | [input](inputs/4_big.c) | [after](results/preferred/add-if-one/4_big/site-004/transformed.c) | [log](results/preferred/add-if-one/4_big/site-004/transform.log) | [verify](results/preferred/add-if-one/4_big/site-004/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 0 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-000/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-000/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-000/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 1 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-001/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-001/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-001/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 2 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-002/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-002/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-002/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 3 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-003/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-003/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-003/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 4 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-004/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-004/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-004/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 5 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-005/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-005/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-005/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 6 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-006/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-006/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-006/verify.log) | ROBUST |
| add-if-one | 5_bubbleSort | 7 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/add-if-one/5_bubbleSort/site-007/transformed.c) | [log](results/preferred/add-if-one/5_bubbleSort/site-007/transform.log) | [verify](results/preferred/add-if-one/5_bubbleSort/site-007/verify.log) | ROBUST |
| add-if-one | 6_pure | 0 | [input](inputs/6_pure.c) | [after](results/preferred/add-if-one/6_pure/site-000/transformed.c) | [log](results/preferred/add-if-one/6_pure/site-000/transform.log) | [verify](results/preferred/add-if-one/6_pure/site-000/verify.log) | ROBUST |
| add-if-one | 6_pure | 1 | [input](inputs/6_pure.c) | [after](results/preferred/add-if-one/6_pure/site-001/transformed.c) | [log](results/preferred/add-if-one/6_pure/site-001/transform.log) | [verify](results/preferred/add-if-one/6_pure/site-001/verify.log) | ROBUST |
| add-if-one | 6_pure | 2 | [input](inputs/6_pure.c) | [after](results/preferred/add-if-one/6_pure/site-002/transformed.c) | [log](results/preferred/add-if-one/6_pure/site-002/transform.log) | [verify](results/preferred/add-if-one/6_pure/site-002/verify.log) | ROBUST |
| for-to-while | 4_big | 0 | [input](inputs/4_big.c) | [after](results/preferred/for-to-while/4_big/site-000/transformed.c) | [log](results/preferred/for-to-while/4_big/site-000/transform.log) | [verify](results/preferred/for-to-while/4_big/site-000/verify.log) | ROBUST |
| for-to-while | 5_bubbleSort | 0 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/for-to-while/5_bubbleSort/site-000/transformed.c) | [log](results/preferred/for-to-while/5_bubbleSort/site-000/transform.log) | [verify](results/preferred/for-to-while/5_bubbleSort/site-000/verify.log) | ROBUST |
| for-to-while | 5_bubbleSort | 1 | [input](inputs/5_bubbleSort.c) | [after](results/preferred/for-to-while/5_bubbleSort/site-001/transformed.c) | [log](results/preferred/for-to-while/5_bubbleSort/site-001/transform.log) | [verify](results/preferred/for-to-while/5_bubbleSort/site-001/verify.log) | ROBUST |
| for-to-while | 6_pure | 0 | [input](inputs/6_pure.c) | [after](results/preferred/for-to-while/6_pure/site-000/transformed.c) | [log](results/preferred/for-to-while/6_pure/site-000/transform.log) | [verify](results/preferred/for-to-while/6_pure/site-000/verify.log) | ROBUST |

## Run

From the repository root:

    ./robustness-benchmarks/pilot/run.sh MODE TRANSFORM REPEAT SEED RUN_ID

Examples:

    ./robustness-benchmarks/pilot/run.sh preferred add-if-zero 1 42 current
    ./robustness-benchmarks/pilot/run.sh multiple add-if-zero 5 42 repeat-5
    ./robustness-benchmarks/pilot/run.sh mixed add-if-zero,add-if-one,for-to-while,deepen-while 5 42 mixed-42
    ./robustness-benchmarks/pilot/run.sh chain add-if-zero,add-if-one,for-to-while,deepen-while 4 42 chain
