void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires \pointer(matrix, m, write);
requires m > 0;
ensures \pointer(matrix, m, write);
ensures m > 0;
ensures (\forall int i; 0 <= i && i < m ==> matrix[i] == 0);
@*/
void zero(int m, int *matrix) {
    /*@
    requires matrix != NULL;
    requires Perm(matrix[i], write);
    ensures Perm(matrix[i], write);
    ensures matrix[i] == 0;
    @*/
    for (int i = 0; i < m; i = i + 1) {
        if (1) {
            matrix[i] = 0;
        } else {
            /*@ assert false; @*/
        }
    }
}