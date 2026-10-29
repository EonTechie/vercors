void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_unique_1__int();
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_unique_2__int();
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_unique_3__int();
@*/

/*@
context_everywhere x0 != NULL;
context_everywhere \pointer_length(x0) == n;
context_everywhere (\forall* int i; 0 <= i && i < n ==> Perm(x0[i], write));
context_everywhere x1 != NULL;
context_everywhere \pointer_length(x1) == n;
context_everywhere (\forall* int i; 0 <= i && i < n ==> Perm(x1[i], 1 \ 2));
context_everywhere x2 != NULL;
context_everywhere \pointer_length(x2) == n;
context_everywhere (\forall* int i; 0 <= i && i < n ==> Perm(x2[i], 1 \ 2));
ensures (\forall int i; 0 <= i && i < n ==> x0[i] == x1[i] + x2[i]);
@*/
int f(int n, unique<1> int *x0, unique<2> int *x1, unique<3> int *x2) {
    if (1) {
        /*@
        loop_invariant 0 <= j;
        loop_invariant j <= n;
        loop_invariant (\forall int i; 0 <= i && i < j ==> x0[i] == x1[i] + x2[i]);
        @*/
        for (int j = 0; j < n; j = j + 1) {
            x0[j] = x1[j] + x2[j];
        }
    } else {
        /*@ assert false; @*/
    }
}