void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

const int IntTag = 0;

const int FloatTag = 1;

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_struct_unknown_152151757();
@*/

struct Element {
    int tag;
};

/*@
decreases;
ensures \result >= 2;
pure int sizeof_pointer_int_();
@*/

/*@
decreases;
ensures \result >= 4;
pure int sizeof_struct_unknown_1994303856();
@*/

struct IElement {
    struct Element e;
    
    int* d;
};

/*@
decreases;
ensures \result >= 2;
pure int sizeof_pointer_float32_();
@*/

/*@
decreases;
ensures \result >= 4;
pure int sizeof_struct_unknown_99015930();
@*/

struct FElement {
    struct Element e;
    
    float* d;
};

void printInt(int v);

void baz() {
    int a;
    a = 1;
    float f;
    struct IElement *E1_196922207;
    (*E1_196922207).e.tag = IntTag;
    (*E1_196922207).d = &a;
    struct FElement *E2_1234080048;
    (*E2_1234080048).e.tag = FloatTag;
    (*E2_1234080048).d = &f;
    /*@ assert Perm(*(*E1_196922207).d, write); @*/
    /*@ assert Perm(*(*E2_1234080048).d, write); @*/
    struct Element **elems;
    elems = (struct Element**) vercorsMalloc(sizeof(struct Element*) * 2);
    /*@ assume elems[0] == (struct Element*)&*E1_196922207; @*/
    /*@ assume elems[1] == (struct Element*)&*E2_1234080048; @*/
    int i;
    i = 0;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 2;
    loop_invariant \pointer(elems, 2, write);
    loop_invariant Perm(*E1_196922207, write);
    loop_invariant (*E1_196922207).d != NULL;
    loop_invariant Perm(*(*E1_196922207).d, write);
    loop_invariant Perm(*E2_1234080048, write);
    loop_invariant (*E2_1234080048).d != NULL;
    loop_invariant Perm(*(*E2_1234080048).d, write);
    loop_invariant elems[0] == (struct Element*)&*E1_196922207;
    loop_invariant elems[1] == (struct Element*)&*E2_1234080048;
    loop_invariant (*elems[0]).tag == IntTag;
    loop_invariant (*elems[1]).tag == FloatTag;
    @*/
    while (i < 2) {
        if ((*elems[i]).tag == IntTag) {
            struct IElement *ie;
            ie = (struct IElement*)elems[i];
            int *ip;
            ip = (*ie).d;
            printInt(*ip);
        }
        i = i + 1;
    }
}