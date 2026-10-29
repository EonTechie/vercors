void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int fibonacci(int n) {
    if (0) {
        /*@ assert false; @*/
    } else {
        if (n > 2) {
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
    }
    return 1;
}