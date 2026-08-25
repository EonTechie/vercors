int isBiggest(int *arr, int length) {
    int biggest;
    biggest = arr[0];
    int i;
    /*@
    loop_invariant 1 <= i;
    loop_invariant i <= length;
    loop_invariant (\forall int x; 0 <= x && x < i ==> biggest >= arr[x]);
    loop_invariant (\exists int x; 0 <= x && x < i && biggest == arr[x]);
    @*/
    for (i = 1; i < length; i = i + 1) {
        if (arr[i] > biggest) {
            if (0) {
                /*@ assert false; @*/
            } else {
                biggest = arr[i];
            }
        }
    }
    return biggest;
}