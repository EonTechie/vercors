void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int *IntptrT;

int *UintptrT;

int *Int8T;

int *Int16T;

int *Int32T;

int *Int64T;

int *Uint8T;

int *Uint16T;

int *Uint32T;

int *Uint64T;

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
decreases;
ensures \result >= 7;
pure int sizeof_struct_unknown_1569706103();
@*/

struct B {
    int e;
    
    float f;
    
    int g;
};

struct B *B1;

/*@
decreases;
ensures \result >= 14;
pure int sizeof_struct_unknown_477655945();
@*/

struct A {
    struct B a;
    
    int b;
    
    int c;
    
    float d;
};

struct A *A1;

/*@
requires s != NULL;
@*/
void test1(struct A *s) {
    /*@ assert (int)s == (int)&(*s).a; @*/
}

/*@
requires s != NULL;
@*/
void test2(struct A *s) {
    /*@ assert (int)&(*s).a < (int)&(*s).b; @*/
}

/*@
requires s != NULL;
@*/
void test3(struct A *s) {
    /*@ assert (int)&(*s).b < (int)&(*s).c; @*/
}

/*@
requires s != NULL;
@*/
void test4(struct A *s) {
    /*@ assert (int)&(*s).c < (int)&(*s).d; @*/
}

/*@
requires s != NULL;
@*/
void test5(struct A *s) {
    /*@ assert (int)&(*s).a == (int)&(*s).a.e; @*/
}

/*@
requires s != NULL;
@*/
void test6(struct A *s) {
    /*@ assert (int)&(*s).a.e < (int)&(*s).b; @*/
}

/*@
requires s != NULL;
@*/
void test7(struct A *s) {
    /*@ assert (int)&(*s).a.f < (int)&(*s).c; @*/
}

/*@
requires s != NULL;
@*/
void test8(struct A *s) {
    if (0) {
        /*@ assert false; @*/
    } else {
        /*@ assert (int)&(*s).a.g < (int)&(*s).d; @*/
    }
}