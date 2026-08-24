/*@
requires (k) >= 0;
requires (m) >= 0;
ensures \result == (k) * (m);
@*/
int mult_add(int k, int m) {
    int sum = 0;
    int remaining = m;
    /*@
    decreases remaining;
    loop_invariant 0 <= (remaining);
    loop_invariant (remaining) <= (m);
    loop_invariant (sum) == ((m) - (remaining)) * (k);
    @*/
    while (0 < (remaining)) {
        if (0) {
            /*@ assert false; @*/
        } else {
            (sum) = (sum) + (k);
        }
        (remaining) = (remaining) - 1;
    }
    return sum;
}