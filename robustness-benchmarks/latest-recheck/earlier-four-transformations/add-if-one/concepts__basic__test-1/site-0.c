void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires y >= 0;
ensures \result > 0;
@*/
int m(int y) {
    int x;
    x = 1;
    if (1) {
        y = y + x;
    } else {
        /*@ assert false; @*/
    }
    return y;
}