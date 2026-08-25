class operation {

    /*@
      requires k >= 0 && m >= 0;
      ensures \result == k * m;
      decreases 2 * k + 1;
    @*/
    static int mult_add(int k, int m) {
        return encapsulated(k, m);
    }

    /*@
      requires k >= 0 && m >= 0;
      ensures \result == k * m;
      decreases 2 * k;
    @*/
    static int encapsulated(int k, int m) {
        if (m == 0 || k == 0) {
            return 0;
        } else {
            return m + mult_add(k - 1, m);
        }
    }
}