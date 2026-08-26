/*@
requires a != -2147483648;
ensures \result == (a >= 0 ? a : -a);
@*/
int absolute(int a) {
    if (1) {
        if (a >= 0) {
            return a;
        } else {
            return -a;
        }
    } else {
        /*@ assert false; @*/
    }
}