int multAdd(int k, int m) {
    if (m == 0 || k == 0) {
        return 0;
    } else {
        if (0) {
            /*@ assert false; @*/
        } else {
            return m + multAdd(k - 1, m);
        }
    }
}