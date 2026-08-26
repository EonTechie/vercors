/*@
requires k >= 0;
requires m >= 0;
decreases k;
ensures \result == k * m;
@*/
int multAdd(int k, int m) {
    if (m == 0 || k == 0) {
        return 0;
    } else {
        if (1) {
            return m + multAdd(k - 1, m);
        } else {
            
        }
    }
}