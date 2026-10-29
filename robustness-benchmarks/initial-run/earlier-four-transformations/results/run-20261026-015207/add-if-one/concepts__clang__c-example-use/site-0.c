void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int *X;

/*@
requires Perm(*X, 1);
ensures Perm(*X, 1);
ensures *X == \old(*X) + 1;
@*/
void test();

/*@
requires Perm(*X, 1);
ensures Perm(*X, 1);
ensures *X == 4;
@*/
void use() {
    if (1) {
        *X = 3;
    } else {
        /*@ assert false; @*/
    }
    test();
}