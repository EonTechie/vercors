void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires expression;
@*/
void assert(_Bool expression);

int printf(int *format);

int test(int x) {
    return x;
}

float test2(float x) {
    return x;
}

/*@
requires y != 0;
ensures \result == x % y;
@*/
int test3(int x, int y) {
    return x % y;
}

/*@
ensures \result == (x + 1E-7 >= y && x - 1E-7 <= y);
@*/
_Bool estimateEq(double x, double y) {
    return x + 1E-7 >= y && x - 1E-7 <= y;
}

int main() {
    test(2.0);
    test2(1);
    assert(1.0 == test3(3.0, 2.0));
    int a;
    int b;
    int c;
    int d;
    int e;
    float w;
    float x;
    float y;
    float z;
    int f;
    f = 5 * 2;
    a = 1;
    x = 1.0;
    y = 1.1;
    assert(1 != 0);
    assert(y >= x);
    assert(y > x);
    assert(x < y);
    assert(x <= y);
    assert(x != y);
    assert(!(x == y));
    assert(1.5 != 1);
    assert(y >= a);
    assert(y > a);
    assert(a < y);
    assert(a <= y);
    assert(a != y);
    assert(!(a == y));
    x = 3.0;
    int bb;
    bb = 2.0;
    a = x / bb;
    a = 3 / 2.0;
    assert(a == 1);
    a = 3.0 / 2;
    assert(a == 1);
    a = 3.0 / 2.0;
    assert(a == 1);
    x = 1;
    assert(x == 3 / 2);
    assert(1.5 != 3 / 2);
    assert(1.5 == 3.0 / 2.0);
    assert(1.5 == 3.0 / 2.0);
    assert(1 != 3.0 / 2.0);
    assert(1 != 3.0 / 2);
    assert(1 != 3 / 2.0);
    assert(3 != 1.9 * 2);
    assert(3.8 == 1.9 * 2);
    assert(3 != 1.9 + 2);
    assert(3.9 == 1.9 + 2);
    assert(0 != 2.9 - 2);
    assert(estimateEq(0.9, 1.9 - 1));
    assert(1 == 3 % 2);
    int sum;
    sum = 22;
    int count;
    count = 5;
    double mean;
    mean = (double)sum / count;
    assert(mean == 4.4);
    int a1;
    int b1;
    b1 = 2;
    float x1;
    x1 = 9.5;
    double y1;
    y1 = 10.5;
    int z1;
    z1 = 50;
    double d1;
    a1 = z1 / b1 + b1 * x1 - y1;
    assert(a1 == 33);
    d1 = z1 / b1 + b1 * x1 - y1;
    if (1) {
        assert(d1 == 33.5);
    } else {
        /*@ assert false; @*/
    }
}