void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

float m() {
    double y;
    y = 0.4;
    float x;
    x = (float)y;
    if (0) {
        /*@ assert false; @*/
    } else {
        y = x + x;
    }
    return 0.8;
}