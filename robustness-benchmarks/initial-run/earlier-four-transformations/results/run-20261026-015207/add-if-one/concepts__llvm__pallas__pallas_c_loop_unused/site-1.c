void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires n >= 0;
@*/
int foo(int n) {
    int oldN;
    oldN = n;
    oldN = oldN + 1;
    if (1) {
        oldN = oldN - 1;
    } else {
        /*@ assert false; @*/
    }
    if (n < 42) {
        return n;
    }
    int res;
    res = 0;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= n + 1;
    loop_invariant res >= 0;
    loop_invariant oldN == n;
    @*/
    for (int i = 0; i <= n; i = i + 1) {
        res = res + i;
    }
    return res;
}