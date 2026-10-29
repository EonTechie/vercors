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
    /*@
    requires ar != NULL;
    requires Perm(ar[i], write);
    ensures ar != NULL;
    ensures Perm(ar[i], write);
    ensures ar[i] == 0;
    @*/
    for (int i = 0; i < len; i = i + 1) {
        if (1) {
            ar[i] = 0;
        } else {
            /*@ assert false; @*/
        }
    }
}