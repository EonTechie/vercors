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

void canCastToInteger() {
    struct B *StructB_1149906013 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_1149906013).structA.integer = 5;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_1149906013;
    /*@ assert *pointerToInteger == 5; @*/
    /*@ assert pointerToInteger == &(*StructB_1149906013).structA.integer; @*/
    /*@ assert pointerToInteger == (int*)&(*StructB_1149906013).structA; @*/
    *pointerToInteger = 10;
    /*@ assert (*StructB_1149906013).structA.integer == 10; @*/
}

void castRemainsValidInLoop() {
    struct B *StructB_1242146015 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_1242146015).structA.integer = 10;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_1242146015;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 10;
    loop_invariant Perm(*StructB_1242146015, write);
    loop_invariant pointerToInteger == (int*)&*StructB_1242146015;
    loop_invariant *pointerToInteger == 10 - i;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        *pointerToInteger = *pointerToInteger - 1;
    }
    /*@ assert (*StructB_1242146015).structA.integer == 0; @*/
    (*StructB_1242146015).structA.integer = 10;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 10;
    loop_invariant Perm(*pointerToInteger, write);
    loop_invariant *pointerToInteger == 10 - i;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        *pointerToInteger = *pointerToInteger - 1;
    }
    /*@ assert (*StructB_1242146015).structA.integer == 0; @*/
}

/*@
requires i < 10;
@*/
inline void func_to_method(int *pointerToInteger, int i) {
    *pointerToInteger = *pointerToInteger - 10;
}

void castRemainsValidInParBlock() {
    struct B *StructB_1713003200 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_1713003200).structA.integer = 10;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_1713003200;
    /*@
    requires i == 8 ==> Perm(*pointerToInteger, write);
    ensures i == 8 ==> Perm(*pointerToInteger, write);
    ensures i == 8 ==> *pointerToInteger == 0;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        if (i == 8) {
            func_to_method(pointerToInteger, i);
        }
    }
    /*@ assert (*StructB_1713003200).structA.integer == 0; @*/
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
    struct B *StructB_774673 = (struct B*) vercorsMallocNonNull(sizeof(struct B));
    (*StructB_774673).structA.integer = 15;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_774673;
    increaseByOne(pointerToInteger);
    /*@ assert (*StructB_774673).structA.integer == 16; @*/
}