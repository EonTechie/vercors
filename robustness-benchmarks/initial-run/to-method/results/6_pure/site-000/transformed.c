void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires 0 <= n;
requires n <= |xs|;
decreases n;
pure int prefixSum(seq<int> xs, int n) = n == 0 ? 0 : prefixSum(xs, n - 1) + xs[n - 1];
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires i < length;
@*/
inline int func_to_method(int sum, int *list, int i, int length) {
    sum = sum + list[i];
    return sum;
}

/*@
given seq<int> xs;
context_everywhere list != NULL;
context_everywhere \pointer(list, length, read);
context_everywhere |xs| == length;
context_everywhere (\forall int x; 0 <= x && x < length ==> xs[x] == list[x]);
requires length >= 0;
ensures \result == prefixSum(xs, length);
@*/
int sumList(int *list, int length) {
    int sum;
    sum = 0;
    int i;
    i = 0;
    /*@
    decreases length - i;
    loop_invariant 0 <= i;
    loop_invariant i <= length;
    loop_invariant sum == prefixSum(xs, i);
    @*/
    for (;i < length; i = i + 1) {
        sum = func_to_method(sum, list, i, length);
    }
    return sum;
}