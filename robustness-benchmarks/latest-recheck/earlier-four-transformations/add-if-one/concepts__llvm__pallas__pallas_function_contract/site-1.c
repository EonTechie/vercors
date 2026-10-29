void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires a >= 0;
requires b >= 0;
ensures a >= -1;
ensures b > -1;
@*/
int foo(int a, int b) {
    return a * b + 1;
}

/*@
requires x < 0;
@*/
int bar(int x) {
    int y;
    y = 1;
    y = y + x;
    if (1) {
        y = y * 42;
    } else {
        /*@ assert false; @*/
    }
    return y;
}