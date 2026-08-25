int multAdd(int k, int m) {
    if (m == 0 || k == 0) {
        if (0) {
            /*@ assert false; @*/
        } else {
            return 0;
        }
    } else {
        return m + multAdd(k - 1, m);
    }
}