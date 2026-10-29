void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int printf(int *format);

/*@
requires expression;
@*/
void assert(_Bool expression);

__opencl_vector_type__(int, 4) *V4si;

__opencl_vector_type__(float, 4) *V4sf;

/*@
requires (\forall int i; i >= 0 && i < 4 ==> 0 <= {: mask[i] :} && {: mask[i] :} < 4);
ensures \result[0] == x[mask[0]];
ensures \result[1] == x[mask[1]];
ensures \result[2] == x[mask[2]];
ensures \result[3] == x[mask[3]];
@*/
__opencl_vector_type__(int, 4) shuffle(__opencl_vector_type__(int, 4) x, __opencl_vector_type__(int, 4) mask) {
    __opencl_vector_type__(int, 4) res;
    res = (__opencl_vector_type__(int, 4))((int)x[mask[0]], (int)x[mask[1]], (int)x[mask[2]], (int)x[mask[3]]);
    return res;
}

/*@
requires 0 <= i;
requires i < 4;
decreases;
pure vector<int,4> unknown(vector<int,4> x, int i, int v) =
    i == 0
        ? vector<int>{v, x[1], x[2], x[3]}
        : i == 1
            ? vector<int>{x[0], v, x[2], x[3]}
            : i == 2 ? vector<int>{x[0], x[1], v, x[3]} : vector<int>{x[0], x[1], x[2], v};
@*/

int main() {
    __opencl_vector_type__(int, 4) x;
    x = (__opencl_vector_type__(int, 4))((int)4, (int)3, (int)2, (int)1);
    __opencl_vector_type__(int, 4) y;
    y = (__opencl_vector_type__(int, 4))((int)1, (int)2, (int)3, (int)4);
    x = y;
    assert(x[0] == 1);
    y = unknown(y, 1, 5);
    y = x + y;
    assert(y[0] == 2 && y[1] == 7 && y[2] == 6 && y[3] == 8);
    y = y * y;
    assert(y[0] == 4 && y[1] == 49 && y[2] == 36 && y[3] == 64);
    y = y - x;
    assert(y[0] == 4 - 1 && y[1] == 49 - 2 && y[2] == 36 - 3 && y[3] == 64 - 4);
    if (1) {
        y = y / x;
    } else {
        /*@ assert false; @*/
    }
    assert(y[0] == (4 - 1) / 1 && y[1] == (49 - 2) / 2 && y[2] == (36 - 3) / 3 && y[3] == (64 - 4) / 4);
    __opencl_vector_type__(int, 4) mask;
    mask = (__opencl_vector_type__(int, 4))((int)0, (int)1, (int)1, (int)3);
    __opencl_vector_type__(int, 4) res;
    res = shuffle(x, mask);
    assert(res[1] == x[1] && res[1] == x[1] && res[2] == x[1] && res[3] == x[3]);
    __opencl_vector_type__(float, 4) z;
    z = (__opencl_vector_type__(float, 4))((float)1, (float)2, (float)3, (float)4);
    float z2;
    z2 = 5;
    z2 = z2 + z2;
    z = z + z;
    return 0;
}