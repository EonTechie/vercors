void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires ptr != NULL;
requires isFloat ==> Perm(*(float*)ptr, write \ 2);
requires !isFloat ==> Perm(*(int*)ptr, write \ 2);
ensures isFloat ==> Perm(*(float*)ptr, write \ 2);
ensures !isFloat ==> Perm(*(int*)ptr, write \ 2);
ensures isFloat ==> \result == *(float*)ptr > 10.0;
ensures !isFloat ==> \result == *(int*)ptr > 10;
@*/
_Bool gt10(void *ptr, _Bool isFloat) {
    if (1) {
        if (isFloat) {
            float f;
            f = *(float*)ptr;
            return f > 10.0;
        } else {
            int i;
            i = *(int*)ptr;
            return i > 10;
        }
    } else {
        /*@ assert false; @*/
    }
}

/*@
ensures \result == a > 10;
@*/
_Bool useInt(int a) {
    return gt10((void*)&a, 0 != 0);
}

/*@
ensures \result == a > 10.0;
@*/
_Bool useFloat(float a) {
    return gt10((void*)&a, 1 == 1);
}