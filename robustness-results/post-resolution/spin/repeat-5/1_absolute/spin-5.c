/*@
requires a != -2147483648;
ensures \result == (a >= 0 ? a : -a);
@*/
int absolute(int a) {
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
                        if (a >= 0) {
                            return a;
                        } else {
                            return -a;
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