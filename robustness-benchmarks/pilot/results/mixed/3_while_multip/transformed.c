/*@
requires k >= 0;
requires m >= 0;
ensures \result == k * m;
@*/
int multAdd(int k, int m) {
    int sum;
    if (0) {
        /*@ assert false; @*/
    } else {
        sum = 0;
    }
    int remaining;
    remaining = m;
    if (1) {
        /*@
        decreases remaining;
        loop_invariant 0 <= remaining;
        loop_invariant remaining <= m;
        loop_invariant sum == (m - remaining) * k;
        @*/
        while (0 < remaining) {
            if (0) {
                /*@ assert false; @*/
            } else {
                if (1) {
                    if (0) {
                        /*@ assert false; @*/
                    } else {
                        sum = sum + k;
                    }
                } else {
                    /*@ assert false; @*/
                }
            }
            remaining = remaining - 1;
        }
    } else {
        /*@ assert false; @*/
    }
    return sum;
}