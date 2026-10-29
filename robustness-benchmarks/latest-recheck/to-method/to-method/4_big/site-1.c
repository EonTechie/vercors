void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires i < length;
@*/
inline int func_to_method(int biggest, int *arr, int i, int length) {
    biggest = arr[i];
    return biggest;
}

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
    /*@
    loop_invariant 1 <= i;
    loop_invariant i <= length;
    loop_invariant (\forall int x; 0 <= x && x < i ==> biggest >= arr[x]);
    loop_invariant (\exists int x; 0 <= x && x < i && biggest == arr[x]);
    @*/
    for (i = 1; i < length; i = i + 1) {
        if (arr[i] > biggest) {
            biggest = func_to_method(biggest, arr, i, length);
        }
    }
    return biggest;
}