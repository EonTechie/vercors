void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires expression;
@*/
void assert(_Bool expression);

__opencl_vector_type__(double, 2) V2df;

__opencl_vector_type__(int, 2) V2di;

__opencl_vector_type__(int, 2) V2du;

__opencl_vector_type__(int, 4) V4si;

__opencl_vector_type__(float, 4) V4sf;

__opencl_vector_type__(int, 4) V4su;

__opencl_vector_type__(int, 8) V8hi;

__opencl_vector_type__(int, 8) V8hu;

__opencl_vector_type__(int, 2) M128i;

__opencl_vector_type__(double, 2) M128d;

__opencl_vector_type__(int, 2) M128iU;

__opencl_vector_type__(double, 2) M128dU;

__opencl_vector_type__(float, 4) M128;

__opencl_vector_type__(float, 4) M128U;

__opencl_vector_type__(float, 4) V4sf1;

__opencl_vector_type__(float, 8) M256;

__opencl_vector_type__(int, 4) M256i;

__opencl_vector_type__(double, 4) M256d;

__opencl_vector_type__(float, 8) M256U;

__opencl_vector_type__(int, 4) M256iU;

__opencl_vector_type__(double, 4) M256dU;

/*@
ensures \result[0] == q0;
ensures \result[1] == q1;
@*/
__opencl_vector_type__(int, 2) mmSetEpi64x(int q1, int q0) {
    __opencl_vector_type__(int, 2) res;
    res = (__opencl_vector_type__(int, 2))((int)q0, (int)q1);
    return res;
}

/*@
ensures \result[0] == A;
ensures \result[1] == A;
@*/
__opencl_vector_type__(int, 2) mmSet1Epi64x(int A) {
    return mmSetEpi64x(A, A);
}

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
ensures P[2] == \result[2];
ensures P[3] == \result[3];
@*/
/*@ pure @*/ __opencl_vector_type__(float, 4) mmLoaduPs(float *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
ensures P[2] == \result[2];
ensures P[3] == \result[3];
@*/
/*@ pure @*/ __opencl_vector_type__(float, 4) mmLoadPs(float *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
@*/
/*@ pure @*/ __opencl_vector_type__(double, 2) mmLoaduPd(double *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
@*/
/*@ pure @*/ __opencl_vector_type__(double, 2) mmLoadPd(double *P);

/*@
decreases;
ensures \result >= 8;
pure int sizeof_int();
@*/

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(((int*)P)[i], read));
ensures ((int*)P)[0] == \result[0];
ensures ((int*)P)[1] == \result[1];
@*/
/*@ pure @*/ __opencl_vector_type__(int, 2) mmLoaduEpi64(void *P);

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(((int*)P)[i], read));
ensures ((int*)P)[0] == \result[0];
ensures ((int*)P)[1] == \result[1];
@*/
/*@ pure @*/ __opencl_vector_type__(int, 2) mmLoadEpi64(void *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 8;
requires (\forall* int i; 0 <= i && i < 8 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
ensures P[2] == \result[2];
ensures P[3] == \result[3];
ensures P[4] == \result[4];
ensures P[5] == \result[5];
ensures P[6] == \result[6];
ensures P[7] == \result[7];
@*/
/*@ pure @*/ __opencl_vector_type__(float, 8) mm256LoaduPs(float *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 8;
requires (\forall* int i; 0 <= i && i < 8 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
ensures P[2] == \result[2];
ensures P[3] == \result[3];
ensures P[4] == \result[4];
ensures P[5] == \result[5];
ensures P[6] == \result[6];
ensures P[7] == \result[7];
@*/
/*@ pure @*/ __opencl_vector_type__(float, 8) mm256LoadPs(float *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
ensures P[2] == \result[2];
ensures P[3] == \result[3];
@*/
/*@ pure @*/ __opencl_vector_type__(double, 4) mm256LoaduPd(double *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], read));
ensures P[0] == \result[0];
ensures P[1] == \result[1];
ensures P[2] == \result[2];
ensures P[3] == \result[3];
@*/
/*@ pure @*/ __opencl_vector_type__(double, 4) mm256LoadPd(double *P);

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(((int*)P)[i], read));
ensures ((int*)P)[0] == \result[0];
ensures ((int*)P)[1] == \result[1];
ensures ((int*)P)[2] == \result[2];
ensures ((int*)P)[3] == \result[3];
@*/
/*@ pure @*/ __opencl_vector_type__(int, 4) mm256LoaduEpi64(void *P);

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(((int*)P)[i], read));
ensures ((int*)P)[0] == \result[0];
ensures ((int*)P)[1] == \result[1];
ensures ((int*)P)[2] == \result[2];
ensures ((int*)P)[3] == \result[3];
@*/
/*@ pure @*/ __opencl_vector_type__(int, 4) mm256LoadEpi64(void *P);

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 4;
ensures (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
ensures P[2] == A[2];
ensures P[3] == A[3];
@*/
void mmStorePs(float *P, __opencl_vector_type__(float, 4) A);

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 4;
ensures (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
ensures P[2] == A[2];
ensures P[3] == A[3];
@*/
void mmStoreuPs(float *P, __opencl_vector_type__(float, 4) A);

/*@
requires P != NULL;
requires \pointer_length(P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 2;
ensures (\forall* int i; 0 <= i && i < 2 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
@*/
void mmStorePd(double *P, __opencl_vector_type__(double, 2) A);

/*@
requires P != NULL;
requires \pointer_length(P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 2;
ensures (\forall* int i; 0 <= i && i < 2 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
@*/
void mmStoreuPd(double *P, __opencl_vector_type__(double, 2) A);

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(((int*)P)[i], write));
ensures P != NULL;
ensures \pointer_length((int*)P) >= 2;
ensures (\forall* int i; 0 <= i && i < 2 ==> Perm(((int*)P)[i], write));
ensures ((int*)P)[0] == A[0];
ensures ((int*)P)[1] == A[1];
@*/
void mmStoreuEpi64(void *P, __opencl_vector_type__(int, 2) A);

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 2;
requires (\forall* int i; 0 <= i && i < 2 ==> Perm(((int*)P)[i], write));
ensures P != NULL;
ensures \pointer_length((int*)P) >= 2;
ensures (\forall* int i; 0 <= i && i < 2 ==> Perm(((int*)P)[i], write));
ensures ((int*)P)[0] == A[0];
ensures ((int*)P)[1] == A[1];
@*/
void mmStoreEpi64(void *P, __opencl_vector_type__(int, 2) A);

/*@
requires P != NULL;
requires \pointer_length(P) >= 8;
requires (\forall* int i; 0 <= i && i < 8 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 8;
ensures (\forall* int i; 0 <= i && i < 8 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
ensures P[2] == A[2];
ensures P[3] == A[3];
ensures P[4] == A[4];
ensures P[5] == A[5];
ensures P[6] == A[6];
ensures P[7] == A[7];
@*/
void mm256StorePs(float *P, __opencl_vector_type__(float, 8) A);

/*@
requires P != NULL;
requires \pointer_length(P) >= 8;
requires (\forall* int i; 0 <= i && i < 8 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 8;
ensures (\forall* int i; 0 <= i && i < 8 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
ensures P[2] == A[2];
ensures P[3] == A[3];
ensures P[4] == A[4];
ensures P[5] == A[5];
ensures P[6] == A[6];
ensures P[7] == A[7];
@*/
void mm256StoreuPs(float *P, __opencl_vector_type__(float, 8) A);

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 4;
ensures (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
ensures P[2] == A[2];
ensures P[3] == A[3];
@*/
void mm256StorePd(double *P, __opencl_vector_type__(double, 4) A);

/*@
requires P != NULL;
requires \pointer_length(P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P != NULL;
ensures \pointer_length(P) >= 4;
ensures (\forall* int i; 0 <= i && i < 4 ==> Perm(P[i], write));
ensures P[0] == A[0];
ensures P[1] == A[1];
ensures P[2] == A[2];
ensures P[3] == A[3];
@*/
void mm256StoreuPd(double *P, __opencl_vector_type__(double, 4) A);

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(((int*)P)[i], write));
ensures P != NULL;
ensures \pointer_length((int*)P) >= 4;
ensures (\forall* int i; 0 <= i && i < 4 ==> Perm(((int*)P)[i], write));
ensures ((int*)P)[0] == A[0];
ensures ((int*)P)[1] == A[1];
ensures ((int*)P)[2] == A[2];
ensures ((int*)P)[3] == A[3];
@*/
void mm256StoreuEpi64(void *P, __opencl_vector_type__(int, 4) A);

/*@
requires P != NULL;
requires \pointer_length((int*)P) >= 4;
requires (\forall* int i; 0 <= i && i < 4 ==> Perm(((int*)P)[i], write));
ensures P != NULL;
ensures \pointer_length((int*)P) >= 4;
ensures (\forall* int i; 0 <= i && i < 4 ==> Perm(((int*)P)[i], write));
ensures ((int*)P)[0] == A[0];
ensures ((int*)P)[1] == A[1];
ensures ((int*)P)[2] == A[2];
ensures ((int*)P)[3] == A[3];
@*/
void mm256StoreEpi64(void *P, __opencl_vector_type__(int, 4) A);

__opencl_vector_type__(float, 4) mmAddPs(__opencl_vector_type__(float, 4) A, __opencl_vector_type__(float, 4) B) {
    return A + B;
}

__opencl_vector_type__(float, 4) mmDivPs(__opencl_vector_type__(float, 4) A, __opencl_vector_type__(float, 4) B) {
    return A / B;
}

__opencl_vector_type__(float, 4) mmMulPs(__opencl_vector_type__(float, 4) A, __opencl_vector_type__(float, 4) B) {
    return A * B;
}

__opencl_vector_type__(float, 4) mmSubPs(__opencl_vector_type__(float, 4) A, __opencl_vector_type__(float, 4) B) {
    return A - B;
}

__opencl_vector_type__(double, 2) mmAddPd(__opencl_vector_type__(double, 2) A, __opencl_vector_type__(double, 2) B) {
    return A + B;
}

__opencl_vector_type__(double, 2) mmDivPd(__opencl_vector_type__(double, 2) A, __opencl_vector_type__(double, 2) B) {
    return A / B;
}

__opencl_vector_type__(double, 2) mmMulPd(__opencl_vector_type__(double, 2) A, __opencl_vector_type__(double, 2) B) {
    return A * B;
}

__opencl_vector_type__(double, 2) mmSubPd(__opencl_vector_type__(double, 2) A, __opencl_vector_type__(double, 2) B) {
    return A - B;
}

__opencl_vector_type__(int, 2) mmAddEpi64(__opencl_vector_type__(int, 2) A, __opencl_vector_type__(int, 2) B) {
    return A + B;
}

/*@
requires B[0] != 0;
requires B[1] != 0;
@*/
__opencl_vector_type__(int, 2) mmDivEpi64(__opencl_vector_type__(int, 2) A, __opencl_vector_type__(int, 2) B) {
    return A / B;
}

__opencl_vector_type__(int, 2) mmMulEpi64(__opencl_vector_type__(int, 2) A, __opencl_vector_type__(int, 2) B) {
    return A * B;
}

__opencl_vector_type__(int, 2) mmSubEpi64(__opencl_vector_type__(int, 2) A, __opencl_vector_type__(int, 2) B) {
    return A - B;
}

__opencl_vector_type__(float, 8) mm256AddPs(__opencl_vector_type__(float, 8) A, __opencl_vector_type__(float, 8) B) {
    return A + B;
}

__opencl_vector_type__(float, 8) mm256DivPs(__opencl_vector_type__(float, 8) A, __opencl_vector_type__(float, 8) B) {
    return A / B;
}

__opencl_vector_type__(float, 8) mm256MulPs(__opencl_vector_type__(float, 8) A, __opencl_vector_type__(float, 8) B) {
    return A * B;
}

__opencl_vector_type__(float, 8) mm256SubPs(__opencl_vector_type__(float, 8) A, __opencl_vector_type__(float, 8) B) {
    return A - B;
}

__opencl_vector_type__(double, 4) mm256AddPd(__opencl_vector_type__(double, 4) A, __opencl_vector_type__(double, 4) B) {
    return A + B;
}

__opencl_vector_type__(double, 4) mm256DivPd(__opencl_vector_type__(double, 4) A, __opencl_vector_type__(double, 4) B) {
    return A / B;
}

__opencl_vector_type__(double, 4) mm256MulPd(__opencl_vector_type__(double, 4) A, __opencl_vector_type__(double, 4) B) {
    return A * B;
}

__opencl_vector_type__(double, 4) mm256SubPd(__opencl_vector_type__(double, 4) A, __opencl_vector_type__(double, 4) B) {
    return A - B;
}

__opencl_vector_type__(int, 4) mm256AddEpi64(__opencl_vector_type__(int, 4) A, __opencl_vector_type__(int, 4) B) {
    return A + B;
}

/*@
requires B[0] != 0;
requires B[1] != 0;
requires B[2] != 0;
requires B[3] != 0;
@*/
__opencl_vector_type__(int, 4) mm256DivEpi64(__opencl_vector_type__(int, 4) A, __opencl_vector_type__(int, 4) B) {
    return A / B;
}

__opencl_vector_type__(int, 4) mm256MulEpi64(__opencl_vector_type__(int, 4) A, __opencl_vector_type__(int, 4) B) {
    return A * B;
}

__opencl_vector_type__(int, 4) mm256SubEpi64(__opencl_vector_type__(int, 4) A, __opencl_vector_type__(int, 4) B) {
    return A - B;
}

/*@
context_everywhere n >= 0;
context_everywhere n % 4 == 0;
context_everywhere a != NULL;
context_everywhere \pointer_length(a) >= n;
context_everywhere b != NULL;
context_everywhere \pointer_length(b) >= n;
context_everywhere c != NULL;
context_everywhere \pointer_length(c) >= n;
context_everywhere (\forall* int i; 0 <= i && i < n ==> Perm(a[i], 1 \ 2));
context_everywhere (\forall* int i; 0 <= i && i < n ==> Perm(b[i], 1 \ 2));
context_everywhere (\forall* int i; 0 <= i && i < n ==> Perm(c[i], write));
ensures (\forall int j; 0 <= j && j < n ==> {: c[j] :} == a[j] + b[j]);
@*/
void vectorAdd(int *a, int *b, int *c, int n) {
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= n;
    loop_invariant i % 4 == 0;
    loop_invariant (\forall int j; 0 <= j && j < i ==> {: c[j] :} == a[j] + b[j]);
    @*/
    for (int i = 0; i < n; i = i + 4) {
        __opencl_vector_type__(int, 4) a4;
        a4 = (__opencl_vector_type__(int, 4))((int)a[i], (int)a[i + 1], (int)a[i + 2], (int)a[i + 3]);
        __opencl_vector_type__(int, 4) b4;
        b4 = (__opencl_vector_type__(int, 4))((int)b[i], (int)b[i + 1], (int)b[i + 2], (int)b[i + 3]);
        __opencl_vector_type__(int, 4) c4;
        c4 = a4 + b4;
        __opencl_vector_type__(int, 4) d4;
        d4 = a4 == b4;
        c[i] = c4[0];
        c[i + 1] = c4[1];
        c[i + 2] = c4[2];
        c[i + 3] = c4[3];
    }
}

int main() {
    int *a;
    a = (int*) vercorsMallocNonNull(sizeof(int) * 4 * 100);
    int *b;
    b = (int*) vercorsMallocNonNull(sizeof(int) * 4 * 100);
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 4 * 100;
    loop_invariant (\forall* int j; 0 <= j && j < 4 * 100 ==> Perm(a[j], write));
    loop_invariant (\forall* int j; 0 <= j && j < 4 * 100 ==> Perm(b[j], write));
    loop_invariant (\forall int j; 0 <= j && j < i ==> {: a[j] :} == j);
    loop_invariant (\forall int j; 0 <= j && j < i ==> {: b[j] :} == 400 - j);
    @*/
    for (int i = 0; i < 4 * 100; i = i + 1) {
        a[i] = i;
        if (1) {
            b[i] = 400 - i;
        } else {
            /*@ assert false; @*/
        }
    }
    int *c;
    c = (int*) vercorsMallocNonNull(sizeof(int) * 4 * 100);
    vectorAdd(a, b, c, 4 * 100);
    /*@ assert (\forall int j; 0 <= j && j < 4 * 100 ==> {: c[j] :} == 400); @*/
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= 4 * 100;
    loop_invariant (\forall* int j; 0 <= j && j < 4 * 100 ==> Perm(c[j], write));
    loop_invariant (\forall int j; 0 <= j && j < 4 * 100 ==> {: c[j] :} == 400);
    @*/
    for (int i = 0; i < 4 * 100; i = i + 1) {
        assert(c[i] == 400);
    }
    return 0;
}