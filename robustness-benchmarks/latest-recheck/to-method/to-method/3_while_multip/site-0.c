void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
context_everywhere \pointer(sum, 1, write);
context_everywhere \pointer(remaining, 1, write);
requires 0 < *remaining;
ensures *sum == \old(*sum) + k;
ensures *remaining == \old(*remaining) - 1;
@*/
void func_to_method(int *sum, int k, int *remaining) {
    *sum = *sum + k;
    *remaining = *remaining - 1;
}

/*@
requires k >= 0;
requires m >= 0;
ensures \result == k * m;
@*/
int multAdd(int k, int m) {
    int sum;
    sum = 0;
    int remaining;
    remaining = m;
    /*@
    decreases remaining;
    loop_invariant \pointer(&sum, 1, write);
    loop_invariant \pointer(&remaining, 1, write);
    loop_invariant 0 <= remaining;
    loop_invariant remaining <= m;
    loop_invariant sum == (m - remaining) * k;
    @*/
    while (0 < remaining) {
        func_to_method(&sum, k, &remaining);
    }
    return sum;
}