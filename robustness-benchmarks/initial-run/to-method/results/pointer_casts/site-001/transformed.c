void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
decreases;
ensures \result >= 3;
pure int sizeof_struct_unknown_1574700923();
@*/

struct A {
    int integer;
    
    _Bool boolean;
};

/*@
decreases;
ensures \result >= 3;
pure int sizeof_struct_unknown_1405534694();
@*/

struct B {
    struct A structA;
};

inline void func_to_method(int *pointerToInteger, struct B *structB) {
    /*@ assert pointerToInteger == &(*structB).structA.integer; @*/
}

void canCastToInteger() {
    struct B *StructB_2035231711 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_2035231711).structA.integer = 5;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_2035231711;
    /*@ assert *pointerToInteger == 5; @*/
    func_to_method(pointerToInteger, StructB_2035231711);
    /*@ assert pointerToInteger == (int*)&(*StructB_2035231711).structA; @*/
    *pointerToInteger = 10;
    /*@ assert (*StructB_2035231711).structA.integer == 10; @*/
}

void castRemainsValidInLoop() {
    struct B *StructB_757201507 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_757201507).structA.integer = 10;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_757201507;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 10;
    loop_invariant Perm(*StructB_757201507, write);
    loop_invariant pointerToInteger == (int*)&*StructB_757201507;
    loop_invariant *pointerToInteger == 10 - i;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        *pointerToInteger = *pointerToInteger - 1;
    }
    /*@ assert (*StructB_757201507).structA.integer == 0; @*/
    (*StructB_757201507).structA.integer = 10;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 10;
    loop_invariant Perm(*pointerToInteger, write);
    loop_invariant *pointerToInteger == 10 - i;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        *pointerToInteger = *pointerToInteger - 1;
    }
    /*@ assert (*StructB_757201507).structA.integer == 0; @*/
}

void castRemainsValidInParBlock() {
    struct B *StructB_1149906013 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_1149906013).structA.integer = 10;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_1149906013;
    /*@
    requires i == 8 ==> Perm(*pointerToInteger, write);
    ensures i == 8 ==> Perm(*pointerToInteger, write);
    ensures i == 8 ==> *pointerToInteger == 0;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        if (i == 8) {
            *pointerToInteger = *pointerToInteger - 10;
        }
    }
    /*@ assert (*StructB_1149906013).structA.integer == 0; @*/
}

/*@
requires a != NULL;
requires Perm(*a, write);
ensures Perm(*a, write);
ensures *a == \old(*a) + 1;
@*/
void increaseByOne(int *a) {
    *a = *a + 1;
}

void callWithCast() {
    struct B *StructB_2112126849 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_2112126849).structA.integer = 15;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_2112126849;
    increaseByOne(pointerToInteger);
    /*@ assert (*StructB_2112126849).structA.integer == 16; @*/
}