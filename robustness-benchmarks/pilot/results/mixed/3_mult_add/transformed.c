/*@
requires k >= 0;
requires m >= 0;
decreases k;
ensures \result == k * m;
@*/
int multAdd(int k, int m) {
    if (0) {
        /*@ assert false; @*/
    } else {
        if (1) {
            if (0) {
                /*@ assert false; @*/
            } else {
                if (0) {
                    /*@ assert false; @*/
                } else {
                    if (1) {
                        if (m == 0 || k == 0) {
                            return 0;
                        } else {
                            return m + multAdd(k - 1, m);
                        }
                    } else {
                        /*@ assert false; @*/
                    }
                }
            }
        } else {
            /*@ assert false; @*/
        }
    }
}