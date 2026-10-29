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
    struct B *StructB_1746488484;
    (*StructB_1746488484).structA.integer = 5;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_1746488484;
    /*@ assert *pointerToInteger == 5; @*/
    /*@ assert pointerToInteger == &(*StructB_1746488484).structA.integer; @*/
    /*@ assert pointerToInteger == (int*)&(*StructB_1746488484).structA; @*/
    *pointerToInteger = 10;
    /*@ assert (*StructB_1746488484).structA.integer == 10; @*/
}

void castRemainsValidInLoop() {
    struct B *StructB_953954861;
    if (1) {
        (*StructB_953954861).structA.integer = 10;
    } else {
        /*@ assert false; @*/
    }
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_953954861;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 10;
    loop_invariant Perm(*StructB_953954861, write);
    loop_invariant pointerToInteger == (int*)&*StructB_953954861;
    loop_invariant *pointerToInteger == 10 - i;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        *pointerToInteger = *pointerToInteger - 1;
    }
    /*@ assert (*StructB_953954861).structA.integer == 0; @*/
    (*StructB_953954861).structA.integer = 10;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 10;
    loop_invariant Perm(*pointerToInteger, write);
    loop_invariant *pointerToInteger == 10 - i;
    @*/
    for (int i = 0; i < 10; i = i + 1) {
        *pointerToInteger = *pointerToInteger - 1;
    }
    /*@ assert (*StructB_953954861).structA.integer == 0; @*/
}

void castRemainsValidInParBlock() {
    struct B *StructB_774673;
    (*StructB_774673).structA.integer = 10;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_774673;
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
    /*@ assert (*StructB_774673).structA.integer == 0; @*/
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
    struct B *StructB_1974321070;
    (*StructB_1974321070).structA.integer = 15;
    int *pointerToInteger;
    pointerToInteger = (int*)&*StructB_1974321070;
    increaseByOne(pointerToInteger);
    /*@ assert (*StructB_1974321070).structA.integer == 16; @*/
}