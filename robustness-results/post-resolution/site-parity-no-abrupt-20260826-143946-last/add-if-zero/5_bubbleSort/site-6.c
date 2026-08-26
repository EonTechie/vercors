/*@
context_everywhere arr != NULL;
context_everywhere \pointer(arr, length, write);
requires length >= 0;
ensures (\forall int i; 0 <= i && i <= length - 2 ==> arr[i] <= arr[i + 1]);
@*/
void bubbleSort(int *arr, int length) {
    int n;
    n = length;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= n;
    loop_invariant (\forall int k; n - i <= k && k < n - 1 ==> arr[k] <= arr[k + 1]);
    loop_invariant (\forall int g, int h; 0 <= g && g < n - i && n - i <= h && h < n ==> arr[g] <= arr[h]);
    @*/
    for (int i = 0; i < n - 1; i = i + 1) {
        /*@
        loop_invariant 0 <= j;
        loop_invariant j <= n - 1 - i;
        loop_invariant (\forall int k; n - i <= k && k < n - 1 ==> arr[k] <= arr[k + 1]);
        loop_invariant (\forall int g, int h; 0 <= g && g < n - i && n - i <= h && h < n ==> arr[g] <= arr[h]);
        loop_invariant (\forall int k; 0 <= k && k < j ==> arr[k] <= arr[j]);
        @*/
        for (int j = 0; j < n - 1 - i; j = j + 1) {
            if (arr[j] > arr[j + 1]) {
                int temp;
                temp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = temp;
            }
            if (0) {
                /*@ assert false; @*/
            } else {
                
            }
        }
    }
}