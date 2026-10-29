void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int X;

/*@
requires Perm(X, 1);
ensures Perm(X, 1);
ensures X == \old(X) + 1;
@*/
void test();

/*@
requires Perm(X, 1);
ensures Perm(X, 1);
ensures X == 4;
@*/
void use() {
    X = 3;
    if (1) {
        test();
    } else {
        /*@ assert false; @*/
    }
}