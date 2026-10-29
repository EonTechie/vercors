void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

void test() {
    int x;
    x = 0;
    int *y;
    y = &x;
    returnPointer(y);
}

int *returnPointer(int *x) {
    return x;
}

/*@
requires \pointer_index(location, 0, write);
ensures \pointer_index(location, 0, write);
ensures *location == 1;
@*/
void setOne(int *location) {
    *location = 1;
}

/*@
ensures \result == 1;
@*/
int returnsOne() {
    int result;
    setOne(&result);
    return result;
}

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires a != b ==> \pointer_index(a, 0, write) ** \pointer_index(b, 0, write);
requires a == b ==> \pointer_index(a, 0, write);
ensures a != b ==> \pointer_index(a, 0, write) ** \pointer_index(b, 0, write);
ensures a == b ==> \pointer_index(a, 0, write);
ensures *a == \old(*b);
ensures *b == \old(*a);
@*/
void swap(int *a, int *b) {
    int tmp;
    tmp = *a;
    *a = *b;
    *b = tmp;
}

void swapSame() {
    int a;
    a = 1;
    swap(&a, &a);
    if (0) {
        /*@ assert false; @*/
    } else {
        /*@ assert a == 1; @*/
    }
}

/*@
requires leftLen > 0;
requires rightLen > 0;
requires \pointer(left, leftLen, write);
requires \pointer(right, rightLen, write);
ensures \pointer(left, leftLen, write);
ensures \pointer(right, rightLen, write);
@*/
void merge(int *left, int leftLen, int *right, int rightLen) {
    
}

/*@
requires len > 0;
requires \pointer(data, len, write);
ensures \pointer(data, len, write);
@*/
void sort(int *data, int len) {
    if (len <= 1) {
        return;
    } else {
        int mid;
        mid = len / 2;
        sort(data, mid);
        sort(data + mid, len - mid);
        merge(data, mid, &data[mid], len - mid);
    }
}