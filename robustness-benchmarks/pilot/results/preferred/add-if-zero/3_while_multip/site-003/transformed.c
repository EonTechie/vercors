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
    loop_invariant 0 <= remaining;
    loop_invariant remaining <= m;
    loop_invariant sum == (m - remaining) * k;
    @*/
    while (0 < remaining) {
        sum = sum + k;
        if (0) {
            /*@ assert false; @*/
        } else {
            remaining = remaining - 1;
        }
    }
    return sum;
}