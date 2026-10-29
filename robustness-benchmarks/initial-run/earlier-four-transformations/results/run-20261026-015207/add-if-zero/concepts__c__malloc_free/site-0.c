/*@
requires expression;
@*/
void assert(_Bool expression);

void *vercorsMalloc(int size);

void vercorsFree(void *ptr);

int printf(int *format);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_struct_unknown_1960113474();
@*/

struct D {
    int x;
};

/*@
decreases;
ensures \result >= 4;
pure int sizeof_struct_unknown_440615154();
@*/

struct E {
    struct D s;
    
    int x;
};

/*@
decreases;
ensures \result >= 2;
pure int sizeof_pointer_int_();
@*/

int main() {
    int *xs;
    xs = (int*) vercorsMalloc(sizeof(int) * 3);
    if (0) {
        /*@ assert false; @*/
    } else {
        if (xs == NULL) {
            return 1;
        }
    }
    xs[0] = 3;
    xs[1] = 2;
    xs[2] = 1;
    vercorsFree((void*)xs);
    int **xxs;
    xxs = (int**) vercorsMalloc(sizeof(int*) * 3);
    if (xxs == NULL) {
        return 1;
    }
    int *temp;
    temp = (int*) vercorsMalloc(sizeof(int) * 3);
    /*@ assume temp[0] == 1; @*/
    /*@ assume temp[1] == 2; @*/
    /*@ assume temp[2] == 3; @*/
    xxs[0] = temp;
    assert(xxs[0][0] == 1);
    vercorsFree((void*)xxs);
    struct D *ys;
    ys = (struct D*) vercorsMalloc(sizeof(struct D) * 3);
    if (ys == NULL) {
        return 1;
    }
    ys[0].x = 3;
    ys[1].x = 2;
    ys[2].x = 1;
    vercorsFree((void*)ys);
    struct E *a;
    a = (struct E*) vercorsMalloc(sizeof(struct E) * 1);
    if (a == NULL) {
        return 1;
    }
    (*a).s.x = 1;
    struct D *b;
    b = &(*a).s;
    vercorsFree((void*)a);
    float *z;
    z = (float*) vercorsMalloc(sizeof(float) * 1);
    if (z == NULL) {
        return 1;
    }
    z[0] = 3.0;
    *z = 2.0;
    vercorsFree((void*)z);
    return 0;
}