void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires \pointer(ar, len, write);
ensures \pointer(ar, len, write);
ensures (\forall int k; 0 <= k && k < len ==> ar[k] == 0);
@*/
void zeroArray(int *ar, int len) {
    int i;
    i = 0;
    /*@
    requires ar != NULL;
    requires Perm(ar[i], write);
    ensures ar != NULL;
    ensures Perm(ar[i], write);
    ensures ar[i] == 0;
    @*/
    while (i < len) {
        ar[i] = 0;
        i = i + 1;
    }
}