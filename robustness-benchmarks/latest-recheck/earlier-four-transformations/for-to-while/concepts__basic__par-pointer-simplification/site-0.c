void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires \pointer(ar, len, write);
ensures \pointer(ar, len, write);
@*/
void test(int *ar, int len) {
    int i;
    i = 0;
    /*@
    requires \pointer_index(ar, i, write);
    ensures \pointer_index(ar, i, write);
    @*/
    while (i < len) {
        i = i + 1;
    }
}