int absolute(int a) {
    if (a >= 0) {
        return a;
    } else {
        if (0) {
            /*@ assert false; @*/
        } else {
            return -a;
        }
    }
}