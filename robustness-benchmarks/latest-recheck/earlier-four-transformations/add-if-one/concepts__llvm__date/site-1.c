void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

_Bool isLater(int y1, int m1, int d1, int y2, int m2, int d2) {
    if (y1 != y2) {
        return y1 > y2;
    } else {
        if (1) {
            if (m1 != m2) {
                return m1 > m2;
            } else {
                return d1 > d2;
            }
        } else {
            /*@ assert false; @*/
        }
    }
}

int test() {
    isLater(2023, 3, 7, 2020, 1, 1);
    isLater(1, 1, 2023, 15, 3, 2023);
    return 0;
}