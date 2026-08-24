/*@
requires (k) >= 0;
requires (m) >= 0;
decreases k;
ensures \result == (k) * (m);
@*/
int mult_add(int k, int m) {
    if (0) {
        /*@ assert false; @*/
    } else {
        if ((m) == 0 || (k) == 0) {
            return 0;
        } else {
            return (m) + (mult_add)((k) - 1, m);
        }
    }
}