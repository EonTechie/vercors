void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int fibonacci(int n) {
    if (1) {
        if (n > 2) {
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
    } else {
        /*@ assert false; @*/
    }
    return 1;
}