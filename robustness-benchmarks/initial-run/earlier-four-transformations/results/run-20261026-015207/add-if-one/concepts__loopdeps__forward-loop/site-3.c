void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
context_everywhere a != NULL;
context_everywhere b != NULL;
context_everywhere c != NULL;
requires \pointer(a, len, write);
requires \pointer(b, len, 1 \ 2);
requires \pointer(c, len, write);
requires (\forall int tid; 0 <= tid && tid < len ==> b[tid] == tid);
ensures \pointer(a, len, write);
ensures \pointer(b, len, 1 \ 2);
ensures \pointer(c, len, write);
ensures (\forall int i; 0 <= i && i < len ==> a[i] == i + 1);
ensures (\forall int i; 0 <= i && i < len ==> b[i] == i);
ensures (\forall int i; 0 < i && i < len ==> c[i] == i + 2);
@*/
void example(int *a, int *b, int *c, int len) {
    /*@
    requires Perm(a[i], write);
    requires Perm(b[i], 1 \ 2);
    requires Perm(c[i], write);
    requires b[i] == i;
    ensures Perm(a[i], 1 \ 2);
    ensures Perm(b[i], 1 \ 2);
    ensures Perm(c[i], write);
    ensures i > 0 ==> Perm(a[i - 1], 1 \ 2);
    ensures i == len - 1 ==> Perm(a[i], 1 \ 2);
    ensures a[i] == i + 1;
    ensures b[i] == i;
    ensures i > 0 ==> c[i] == i + 2;
    @*/
    for (int i = 0; i < len; i = i + 1) {
        a[i] = b[i] + 1;
        if (1) {
            /*@ send s, 1: 0 <= i ** i < len ** Perm(a[i], 1 \ 2) ** a[i] == i + 1; @*/
        } else {
            /*@ assert false; @*/
        }
        /*@ recv s; @*/
        S2:
        if (i > 0) {
            c[i] = a[i - 1] + 2;
        }
    }
}