void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

float m() {
    double y;
    y = 0.4;
    float x;
    x = (float)y;
    if (1) {
        y = x + x;
    } else {
        /*@ assert false; @*/
    }
    return 0.8;
}