void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int X;

int X1;

/*@
requires Perm(X, 1);
ensures Perm(X, 1);
ensures X == \old(X) + 1;
@*/
void test() {
    if (1) {
        X = X + 1;
    } else {
        /*@ assert false; @*/
    }
}