/*@
context_everywhere \pointer(arr, length, read);
requires length > 0;
ensures (\forall int x; 0 <= x && x < length ==> \result >= arr[x]);
ensures (\exists int x; 0 <= x && x < length && \result == arr[x]);
@*/
int isBiggest(int *arr, int length) {
    int biggest;
    biggest = arr[0];
    int i;
    i = 1;
    /*@
    loop_invariant 1 <= i;
    loop_invariant i <= length;
    loop_invariant (\forall int x; 0 <= x && x < i ==> biggest >= arr[x]);
    loop_invariant (\exists int x; 0 <= x && x < i && biggest == arr[x]);
    @*/
    while (i < length) {
        if (0) {
            /*@ assert false; @*/
        } else {
            if (0) {
                /*@ assert false; @*/
            } else {
                if (1) {
                    if (1) {
                        if (arr[i] > biggest) {
                            biggest = arr[i];
                        }
                    } else {
                        /*@ assert false; @*/
                    }
                    i = i + 1;
                } else {
                    /*@ assert false; @*/
                }
            }
        }
    }
    return biggest;
}