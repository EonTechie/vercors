/*@
context_everywhere \pointer(arr, length, read);
requires length > 0;
ensures (\forall int x; 0 <= x && x < length ==> \result >= arr[x]);
ensures (\exists int x; 0 <= x && x < length && \result == arr[x]);
@*/
int isBiggest(int *arr, int length) {
    int biggest;
    if (1) {
        if (1) {
            if (1) {
                if (1) {
                    biggest = arr[0];
                } else {
                    /*@ assert false; @*/
                }
            } else {
                /*@ assert false; @*/
            }
        } else {
            /*@ assert false; @*/
        }
    } else {
        /*@ assert false; @*/
    }
    int i;
    if (1) {
        /*@
        loop_invariant 1 <= i;
        loop_invariant i <= length;
        loop_invariant (\forall int x; 0 <= x && x < i ==> biggest >= arr[x]);
        loop_invariant (\exists int x; 0 <= x && x < i && biggest == arr[x]);
        @*/
        for (i = 1; i < length; i = i + 1) {
            if (arr[i] > biggest) {
                biggest = arr[i];
            }
        }
    } else {
        /*@ assert false; @*/
    }
    return biggest;
}