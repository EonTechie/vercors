void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires i != NULL;
requires res != NULL;
requires n != NULL;
requires Perm(*i, write);
requires Perm(*res, write);
requires Perm(*n, read);
requires *i >= 0;
requires *i <= *n;
requires *res == *i * *n * *n;
ensures i != NULL;
ensures res != NULL;
ensures n != NULL;
ensures Perm(*i, write);
ensures Perm(*res, write);
ensures Perm(*n, read);
ensures *i >= 0;
ensures *i <= *n;
ensures *res == *i * *n * *n;
@*/
void cubedLoop1InvariantAssert(int *i, int *res, int *n);

/*@
ensures i != NULL;
ensures res != NULL;
ensures n != NULL;
ensures Perm(*i, write);
ensures Perm(*res, write);
ensures Perm(*n, read);
ensures *i >= 0;
ensures *i <= *n;
ensures *res == *i * *n * *n;
@*/
void cubedLoop1InvariantAssume(int *i, int *res, int *n);

/*@
requires n >= 0;
ensures \result == n * n * n;
@*/
int cubed(int n) {
    int i;
    i = 0;
    int res;
    res = 0;
    cubedLoop1InvariantAssert(&i, &res, &n);
    while (1 == 1) {
        if (0) {
            /*@ assert false; @*/
        } else {
            cubedLoop1InvariantAssume(&i, &res, &n);
            if (i >= n) {
                break;
            }
            res = res + n * n;
            i = i + 1;
            cubedLoop1InvariantAssert(&i, &res, &n);
        }
    }
    return res;
}

/*@
decreases;
ensures \result >= 4;
pure int sizeof_struct_unknown_1365773070();
@*/

struct TestStruct {
    int a;
    
    int b;
};

/*@
ensures \result == 10;
@*/
int complicatedFunction() {
    struct TestStruct *A_491793772;
    (*A_491793772).a = 10;
    return (*A_491793772).a;
}

/*@
ensures \result == 10;
@*/
int main(int argc, int **argv) {
    return complicatedFunction();
}