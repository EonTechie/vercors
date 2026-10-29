void *vercorsMallocNonNull(int size);
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

inline void func_to_method(struct IElement *e1) {
    /*@ assert Perm(*(*e1).d, write); @*/
}

void baz() {
    int a;
    a = 1;
    float f;
    struct IElement *E1_1930629187 = (struct IElement*) vercorsMallocNonNull(sizeof(struct IElement));
    (*E1_1930629187).e.tag = IntTag;
    (*E1_1930629187).d = &a;
    struct FElement *E2_95366086 = (struct FElement*) vercorsMallocNonNull(sizeof(struct FElement));
    (*E2_95366086).e.tag = FloatTag;
    (*E2_95366086).d = &f;
    func_to_method(E1_1930629187);
    /*@ assert Perm(*(*E2_95366086).d, write); @*/
    struct Element **elems;
    elems = (struct Element**) vercorsMallocNonNull(sizeof(struct Element*) * 2);
    /*@ assume elems[0] == (struct Element*)&*E1_1930629187; @*/
    /*@ assume elems[1] == (struct Element*)&*E2_95366086; @*/
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 2;
    loop_invariant \pointer(elems, 2, write);
    loop_invariant Perm(*E1_1930629187, write);
    loop_invariant (*E1_1930629187).d != NULL;
    loop_invariant Perm(*(*E1_1930629187).d, write);
    loop_invariant Perm(*E2_95366086, write);
    loop_invariant (*E2_95366086).d != NULL;
    loop_invariant Perm(*(*E2_95366086).d, write);
    loop_invariant elems[0] == (struct Element*)&*E1_1930629187;
    loop_invariant elems[1] == (struct Element*)&*E2_95366086;
    loop_invariant (*elems[0]).tag == IntTag;
    loop_invariant (*elems[1]).tag == FloatTag;
    @*/
    for (int i = 0; i < 2; i = i + 1) {
        if ((*elems[i]).tag == IntTag) {
            struct IElement *ie;
            ie = (struct IElement*)elems[i];
            int *ip;
            ip = (*ie).d;
            printInt(*ip);
        }
    }
}