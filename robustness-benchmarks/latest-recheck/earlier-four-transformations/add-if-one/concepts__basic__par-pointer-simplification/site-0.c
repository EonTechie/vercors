void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires \pointer(ar, len, write);
ensures \pointer(ar, len, write);
@*/
void test(int *ar, int len) {
    if (1) {
        /*@
        requires \pointer_index(ar, i, write);
        ensures \pointer_index(ar, i, write);
        @*/
        for (int i = 0; i < len; i = i + 1) {
            
        }
    } else {
        /*@ assert false; @*/
    }
}