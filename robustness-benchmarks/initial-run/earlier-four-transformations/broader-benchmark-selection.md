# Broader benchmark selection

This document lists the 88 C examples reviewed and explains which programs were
selected for the shared benchmark of add-if-zero, add-if-one, for-to-while and
deepen-while.

The 34 selected programs had successful original verification in our runs and
were not expected-error tests. They form a shared input set; inclusion does not
mean that every transformation has an applicable site in every program.

The selected copies are in [inputs/](inputs/), their repository paths are in
[SOURCES.tsv](SOURCES.tsv), and supporting logs are in
[original-verification/](original-verification/). Transformation results are
linked from the [benchmark overview](broader-benchmark.md).
The seven-program [pilot set](../../pilot/pilot-benchmark.md) is separate.

## Selected programs (34)

| File | Selection reason |
|---|---|
| [examples/archive/known-problems/853/floats.c](../../../examples/archive/known-problems/853/floats.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/arrays/basic-examples.c](../../../examples/concepts/arrays/basic-examples.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/basic/par-pointer-simplification.c](../../../examples/concepts/basic/par-pointer-simplification.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/basic/test-1.c](../../../examples/concepts/basic/test-1.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/casts.c](../../../examples/concepts/c/casts.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/floats.c](../../../examples/concepts/c/floats.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/malloc_free.c](../../../examples/concepts/c/malloc_free.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/math.c](../../../examples/concepts/c/math.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/mod_div.c](../../../examples/concepts/c/mod_div.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/pointer_casts.c](../../../examples/concepts/c/pointer_casts.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/pointer_relations.c](../../../examples/concepts/c/pointer_relations.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/pointer_tests.c](../../../examples/concepts/c/pointer_tests.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/ptr_comparisons.c](../../../examples/concepts/c/ptr_comparisons.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/structs.c](../../../examples/concepts/c/structs.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/tagged_struct.c](../../../examples/concepts/c/tagged_struct.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/vector_add.c](../../../examples/concepts/c/vector_add.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/vector_type.c](../../../examples/concepts/c/vector_type.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/c/void.c](../../../examples/concepts/c/void.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/clang/c-example-impl.c](../../../examples/concepts/clang/c-example-impl.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/clang/c-example-use.c](../../../examples/concepts/clang/c-example-use.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/llvm/cantor.c](../../../examples/concepts/llvm/cantor.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/llvm/cubed.c](../../../examples/concepts/llvm/cubed.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/llvm/date.c](../../../examples/concepts/llvm/date.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/llvm/fib.c](../../../examples/concepts/llvm/fib.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/llvm/pallas/pallas_c_loop_unused.c](../../../examples/concepts/llvm/pallas/pallas_c_loop_unused.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/llvm/pallas/pallas_function_contract.c](../../../examples/concepts/llvm/pallas/pallas_function_contract.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/loopdeps/forward-loop.c](../../../examples/concepts/loopdeps/forward-loop.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/parallel/zero-loop.c](../../../examples/concepts/parallel/zero-loop.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/parallel/zero-sub-array.c](../../../examples/concepts/parallel/zero-sub-array.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/pointerArithmetic/pointerSubtract.c](../../../examples/concepts/pointerArithmetic/pointerSubtract.c) | Selected: original verification passed; not an expected-error test. |
| [examples/concepts/unique/arrays.c](../../../examples/concepts/unique/arrays.c) | Selected: original verification passed; not an expected-error test. |
| [examples/technical/keywords/allowed-c.c](../../../examples/technical/keywords/allowed-c.c) | Selected: original verification passed; not an expected-error test. |
| [examples/technical/labeledEmbeddedStatement.c](../../../examples/technical/labeledEmbeddedStatement.c) | Selected: original verification passed; not an expected-error test. |
| [examples/verifythis/2025/Challenge2.c](../../../examples/verifythis/2025/Challenge2.c) | Selected: original verification passed; not an expected-error test. |

## Programs not selected (54)

| File | Selection reason |
|---|---|
| [examples/archive/known-problems/791/access-sub-matrix-fail.c](../../../examples/archive/known-problems/791/access-sub-matrix-fail.c) | Not selected: original verification exceeded the time limit. |
| [examples/archive/known-problems/791/access-sub-matrix-pass.c](../../../examples/archive/known-problems/791/access-sub-matrix-pass.c) | Not selected: original verification did not complete successfully. Pointer outside the allocated region. |
| [examples/archive/known-problems/791/zero-sub-matrix-par.c](../../../examples/archive/known-problems/791/zero-sub-matrix-par.c) | Not selected: original verification did not complete successfully. Pointer outside the allocated region. |
| [examples/archive/known-problems/791/zero-sub-matrix.c](../../../examples/archive/known-problems/791/zero-sub-matrix.c) | Not selected: original verification did not complete successfully. Pointer outside the allocated region. |
| [examples/archive/known-problems/826/histogram-matrix.c](../../../examples/archive/known-problems/826/histogram-matrix.c) | Not selected: original verification did not complete successfully. Parsing failed: Reducible is unsupported. |
| [examples/archive/known-problems/826/histogram-submatrix.c](../../../examples/archive/known-problems/826/histogram-submatrix.c) | Not selected: original verification did not complete successfully. Parsing failed: Reducible is unsupported. |
| [examples/archive/known-problems/826/summation.c](../../../examples/archive/known-problems/826/summation.c) | Not selected: original verification did not complete successfully. Parsing failed: Reducible is unsupported. |
| [examples/archive/known-problems/856/opencl.c](../../../examples/archive/known-problems/856/opencl.c) | Not selected: original verification did not complete successfully. Unsupported GPU kernel parameter type. |
| [examples/archive/known-problems/856/opencl_incr.c](../../../examples/archive/known-problems/856/opencl_incr.c) | Not selected: original verification did not complete successfully. Unsupported GPU kernel parameter type. |
| [examples/concepts/arrays/zero-array-ic-e1.c](../../../examples/concepts/arrays/zero-array-ic-e1.c) | Not selected: expected-error test. |
| [examples/concepts/bitvectors/basic.c](../../../examples/concepts/bitvectors/basic.c) | Not selected: original verification did not complete successfully. Could not determine the bit width of a bitwise operation. |
| [examples/concepts/c/integer_bounds.c](../../../examples/concepts/c/integer_bounds.c) | Not selected: original verification did not complete successfully. Assertion might not hold. |
| [examples/concepts/c/mismatched_provenance.c](../../../examples/concepts/c/mismatched_provenance.c) | Not selected: expected-error test. |
| [examples/concepts/c/pointer_array.c](../../../examples/concepts/c/pointer_array.c) | Not selected: original verification did not complete successfully. Pointer outside the allocated region. |
| [examples/concepts/c/pointer_tag.c](../../../examples/concepts/c/pointer_tag.c) | Not selected: original verification did not complete successfully. Could not determine the bit width of a bitwise operation. |
| [examples/concepts/c/xor_linked_list.c](../../../examples/concepts/c/xor_linked_list.c) | Not selected: original verification did not complete successfully. Could not determine the bit width of a bitwise operation. |
| [examples/concepts/llvm/pallas/extContracts/pallas_c_genContrAssume.c](../../../examples/concepts/llvm/pallas/extContracts/pallas_c_genContrAssume.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/ghost/pallas_c_ghost_arg.c](../../../examples/concepts/llvm/pallas/ghost/pallas_c_ghost_arg.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/ghost/pallas_ghost_func.c](../../../examples/concepts/llvm/pallas/ghost/pallas_ghost_func.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_assert.c](../../../examples/concepts/llvm/pallas/pallas_c_assert.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_assert_fail.c](../../../examples/concepts/llvm/pallas/pallas_c_assert_fail.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_assume.c](../../../examples/concepts/llvm/pallas/pallas_c_assume.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_fibonacci.c](../../../examples/concepts/llvm/pallas/pallas_c_fibonacci.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_linked_list.c](../../../examples/concepts/llvm/pallas/pallas_c_linked_list.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_lower_bound.c](../../../examples/concepts/llvm/pallas/pallas_c_lower_bound.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_multiply.c](../../../examples/concepts/llvm/pallas/pallas_c_multiply.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_old.c](../../../examples/concepts/llvm/pallas/pallas_c_old.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_old_fail.c](../../../examples/concepts/llvm/pallas/pallas_c_old_fail.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_perm.c](../../../examples/concepts/llvm/pallas/pallas_c_perm.c) | Not selected: original verification did not complete successfully. Unresolved name: `_Perm`. |
| [examples/concepts/llvm/pallas/pallas_c_perm_fail_1.c](../../../examples/concepts/llvm/pallas/pallas_c_perm_fail_1.c) | Not selected: original verification did not complete successfully. Unresolved name: `_Perm`. |
| [examples/concepts/llvm/pallas/pallas_c_perm_fail_2.c](../../../examples/concepts/llvm/pallas/pallas_c_perm_fail_2.c) | Not selected: original verification did not complete successfully. Insufficient permission to access the location. |
| [examples/concepts/llvm/pallas/pallas_c_perm_fail_3.c](../../../examples/concepts/llvm/pallas/pallas_c_perm_fail_3.c) | Not selected: original verification did not complete successfully. Unresolved name: `_Perm`. |
| [examples/concepts/llvm/pallas/pallas_c_pred.c](../../../examples/concepts/llvm/pallas/pallas_c_pred.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_quantifier.c](../../../examples/concepts/llvm/pallas/pallas_c_quantifier.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_quantifier_fail.c](../../../examples/concepts/llvm/pallas/pallas_c_quantifier_fail.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_c_square_fail.c](../../../examples/concepts/llvm/pallas/pallas_c_square_fail.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pallas/pallas_function_contract_fail.c](../../../examples/concepts/llvm/pallas/pallas_function_contract_fail.c) | Not selected: original verification did not complete successfully. Postcondition might not hold. |
| [examples/concepts/llvm/pallas/pallas_loop_goto.c](../../../examples/concepts/llvm/pallas/pallas_loop_goto.c) | Not selected: original verification did not complete successfully. Parsing failed at `@*/`. |
| [examples/concepts/llvm/pallas/pallas_result.c](../../../examples/concepts/llvm/pallas/pallas_result.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/pointer_casts.c](../../../examples/concepts/llvm/pointer_casts.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/llvm/structs.c](../../../examples/concepts/llvm/structs.c) | Not selected: original verification did not complete successfully. Parsing failed at `*/`. |
| [examples/concepts/loopdeps/backward-dep-e1.c](../../../examples/concepts/loopdeps/backward-dep-e1.c) | Not selected: expected-error test. |
| [examples/concepts/loopdeps/forward-dep-e1.c](../../../examples/concepts/loopdeps/forward-dep-e1.c) | Not selected: expected-error test. |
| [examples/concepts/openmp/add-spec-simd.c](../../../examples/concepts/openmp/add-spec-simd.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/concepts/openmp/add-spec.c](../../../examples/concepts/openmp/add-spec.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/concepts/openmp/copy-spec.c](../../../examples/concepts/openmp/copy-spec.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/concepts/openmp/init-spec.c](../../../examples/concepts/openmp/init-spec.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/concepts/openmp/sections-reduced-fail.c](../../../examples/concepts/openmp/sections-reduced-fail.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/concepts/openmp/sections-reduced.c](../../../examples/concepts/openmp/sections-reduced.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/concepts/openmp/sum-spec.c](../../../examples/concepts/openmp/sum-spec.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/concepts/openmp/test-main.c](../../../examples/concepts/openmp/test-main.c) | Not selected: original verification did not complete successfully. Preprocessing failed: `unistd.h` was unavailable. |
| [examples/concepts/openmp/test-other.c](../../../examples/concepts/openmp/test-other.c) | Not selected: original verification did not complete successfully. Preprocessing failed: `unistd.h` was unavailable. |
| [examples/concepts/openmp/zero-spec.c](../../../examples/concepts/openmp/zero-spec.c) | Not selected: original verification did not complete successfully. Unsupported C expression in the front end. |
| [examples/technical/keywords/disallowed-c-inline.c](../../../examples/technical/keywords/disallowed-c-inline.c) | Not selected: original verification did not complete successfully. `inline` occurs in an empty declaration. |
