/*@
context_everywhere arr != NULL;
context_everywhere \pointer(arr, length, write);
requires length >= 0;
ensures (\forall int i; 0 <= i && i <= length - 2 ==> arr[i] <= arr[i + 1]);
@*/
void bubbleSort(int *arr, int length) {
    int n;
    n = length;
    int i;
    i = 0;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= n;
    loop_invariant (\forall int k; n - i <= k && k < n - 1 ==> arr[k] <= arr[k + 1]);
    loop_invariant (\forall int g, int h; 0 <= g && g < n - i && n - i <= h && h < n ==> arr[g] <= arr[h]);
    @*/
    while (i < n - 1) {
        if (1) {
            int j;
            j = 0;
            /*@
            loop_invariant 0 <= j;
            loop_invariant j <= n - 1 - i;
            loop_invariant (\forall int k; n - i <= k && k < n - 1 ==> arr[k] <= arr[k + 1]);
            loop_invariant (\forall int g, int h; 0 <= g && g < n - i && n - i <= h && h < n ==> arr[g] <= arr[h]);
            loop_invariant (\forall int k; 0 <= k && k < j ==> arr[k] <= arr[j]);
            @*/
            while (j < n - 1 - i) {
                if (arr[j] > arr[j + 1]) {
                    if (0) {
                        /*@ assert false; @*/
                    } else {
                        int temp;
                        if (0) {
                            /*@ assert false; @*/
                        } else {
                            temp = arr[j];
                        }
                        arr[j] = arr[j + 1];
                        arr[j + 1] = temp;
                    }
                }
                j = j + 1;
            }
        } else {
            /*@ assert false; @*/
        }
        i = i + 1;
    }
}