void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires \pointer(ar, len, write);
ensures \pointer(ar, len, write);
ensures (\forall int k; 0 <= k && k < len ==> ar[k] == 0);
@*/
void zeroArray(int *ar, int len) {
    /*@
    requires ar != NULL;
    requires Perm(ar[i], write);
    ensures ar != NULL;
    ensures Perm(ar[i], write);
    ensures ar[i] == 0;
    @*/
    for (int i = 0; i < len; i = i + 1) {
        ar[i] = 0;
    }
}

/*@
requires \pointer(a, len, write);
ensures \pointer(a, len, write);
ensures (\forall int i; 0 <= i && i < len ==> a[i] == \old(a[i]));
@*/
void copyArray(int *a, int len) {
    /*@
    requires a != NULL;
    requires Perm(a[i], write);
    requires a[i] == \old(a[i]);
    ensures a != NULL;
    ensures Perm(a[i], write);
    ensures a[i] == \old(a[i]);
    @*/
    for (int i = 0; i < len; i = i + 1) {
        a[i] = a[i];
    }
}

/*@
requires \pointer(a, len, write);
requires \pointer(b, len, 1 \ 2);
requires \pointer(c, len, 1 \ 2);
ensures \pointer(a, len, write);
ensures \pointer(b, len, 1 \ 2);
ensures \pointer(c, len, 1 \ 2);
ensures (\forall int i; 0 <= i && i < len ==> a[i] == b[i] + c[i]);
ensures (\forall int i; 0 <= i && i < len ==> b[i] == \old(b[i]));
ensures (\forall int i; 0 <= i && i < len ==> c[i] == \old(c[i]));
@*/
void vectorAdd(int *a, int *b, int *c, int len) {
    /*@
    requires a != NULL;
    requires b != NULL;
    requires c != NULL;
    requires Perm(a[i], write);
    requires Perm(b[i], 1 \ 2);
    requires Perm(c[i], 1 \ 2);
    requires b[i] == \old(b[i]);
    requires c[i] == \old(c[i]);
    ensures a != NULL;
    ensures b != NULL;
    ensures c != NULL;
    ensures Perm(a[i], write);
    ensures Perm(b[i], 1 \ 2);
    ensures Perm(c[i], 1 \ 2);
    ensures b[i] == \old(b[i]);
    ensures c[i] == \old(c[i]);
    ensures a[i] == b[i] + c[i];
    @*/
    for (int i = 0; i < len; i = i + 1) {
        a[i] = b[i] + c[i];
    }
}

/*@
requires \pointer(a, n, write);
requires \pointer(b, n, 1 \ 2);
requires \pointer(c, n, write);
ensures \pointer(a, n, write);
ensures \pointer(b, n, 1 \ 2);
ensures \pointer(c, n, write);
@*/
void indepDrf(int *a, int *b, int *c, int n) {
    /*@
    requires a != NULL;
    requires b != NULL;
    requires c != NULL;
    requires Perm(a[i], write);
    requires Perm(c[i], write);
    requires Perm(b[i], 1 \ 2);
    ensures a != NULL;
    ensures b != NULL;
    ensures c != NULL;
    ensures Perm(a[i], write);
    ensures Perm(c[i], write);
    ensures Perm(b[i], 1 \ 2);
    @*/
    for (int i = 0; i < n; i = i + 1) {
        if (1) {
            a[i] = b[i] + 1;
        } else {
            /*@ assert false; @*/
        }
        c[i] = a[i] + 2;
    }
}

/*@
requires \pointer(a, n, write);
requires \pointer(b, n, 1 \ 2);
requires \pointer(c, n, write);
ensures \pointer(a, n, write);
ensures \pointer(b, n, 1 \ 2);
ensures \pointer(c, n, write);
@*/
void forwardDrf(int *a, int *b, int *c, int n) {
    /*@
    requires a != NULL;
    requires b != NULL;
    requires c != NULL;
    requires Perm(a[i], write);
    requires Perm(b[i], 1 \ 2);
    requires Perm(c[i], write);
    ensures a != NULL;
    ensures b != NULL;
    ensures c != NULL;
    ensures Perm(a[i], 1 \ 2);
    ensures Perm(b[i], 1 \ 2);
    ensures Perm(c[i], write);
    ensures i > 0 ==> Perm(a[i - 1], 1 \ 2);
    ensures i == n - 1 ==> Perm(a[i], 1 \ 2);
    @*/
    for (int i = 0; i < n; i = i + 1) {
        a[i] = b[i] + 1;
        /*@ send s, 1: a != NULL ** 0 <= i ** i < n - 1 ** Perm(a[i], 1 \ 2); @*/
        /*@ recv s; @*/
        if (i > 0) {
            c[i] = a[i - 1] + 2;
        }
    }
}

/*@
requires \pointer(a, len, write);
requires \pointer(b, len, 1 \ 2);
requires \pointer(c, len, write);
requires (\forall int i; 0 <= i && i < len ==> b[i] == i);
ensures \pointer(a, len, write);
ensures \pointer(b, len, 1 \ 2);
ensures \pointer(c, len, write);
ensures (\forall int i; 0 <= i && i < len ==> b[i] == i);
ensures (\forall int i; 0 <= i && i < len ==> a[i] == i + 1);
ensures (\forall int i; 0 < i && i < len ==> c[i] == i + 2);
@*/
void forwardFull(int *a, int *b, int *c, int len) {
    /*@
    requires a != NULL;
    requires b != NULL;
    requires c != NULL;
    requires Perm(a[i], write);
    requires Perm(b[i], 1 \ 2);
    requires Perm(c[i], write);
    requires b[i] == i;
    ensures a != NULL;
    ensures b != NULL;
    ensures c != NULL;
    ensures Perm(a[i], 1 \ 2);
    ensures Perm(b[i], 1 \ 2);
    ensures Perm(c[i], write);
    ensures i > 0 ==> Perm(a[i - 1], 1 \ 2);
    ensures i == len - 1 ==> Perm(a[i], 1 \ 2);
    ensures a[i] == i + 1;
    ensures b[i] == i;
    ensures i > 0 ==> c[i] == i + 2;
    @*/
    for (int i = 0; i < len; i = i + 1) {
        a[i] = b[i] + 1;
        /*@ send fs, 1: a != NULL ** 0 <= i ** i < len - 1 ** Perm(a[i], 1 \ 2) ** a[i] == i + 1; @*/
        /*@ recv fs; @*/
        if (i > 0) {
            c[i] = a[i - 1] + 2;
        }
    }
}

/*@
requires \pointer(a, n, write);
requires \pointer(b, n, 1 \ 2);
requires \pointer(c, n, write);
ensures \pointer(a, n, write);
ensures \pointer(b, n, 1 \ 2);
ensures \pointer(c, n, write);
@*/
void backwardDrf(int *a, int *b, int *c, int n) {
    /*@
    requires a != NULL;
    requires b != NULL;
    requires c != NULL;
    requires Perm(a[i], 1 \ 2);
    requires Perm(b[i], 1 \ 2);
    requires Perm(c[i], write);
    requires i == 0 ==> Perm(a[i], 1 \ 2);
    requires i < n - 1 ==> Perm(a[i + 1], 1 \ 2);
    ensures a != NULL;
    ensures b != NULL;
    ensures c != NULL;
    ensures Perm(a[i], 1 \ 2);
    ensures Perm(a[i], 1 \ 2);
    ensures Perm(b[i], 1 \ 2);
    ensures Perm(c[i], write);
    @*/
    for (int i = 0; i < n; i = i + 1) {
        /*@ recv t; @*/
        a[i] = b[i] + 1;
        if (i < n - 1) {
            c[i] = a[i + 1] + 2;
        }
        /*@ send t, 1: a != NULL ** 0 <= i ** i < n - 1 ** Perm(a[i + 1], 1 \ 2); @*/
    }
}

/*@
requires \pointer(a, len, write);
requires \pointer(b, len, 1 \ 2);
requires \pointer(c, len, write);
requires (\forall int tid; 0 <= tid && tid < len ==> a[tid] == 0);
requires (\forall int tid; 0 <= tid && tid < len ==> b[tid] == tid);
ensures \pointer(a, len, write);
ensures \pointer(b, len, 1 \ 2);
ensures \pointer(c, len, write);
ensures (\forall int i; 0 <= i && i < len ==> a[i] == i + 1);
ensures (\forall int i; 0 <= i && i < len ==> b[i] == i);
ensures (\forall int i; 0 <= i && i < len - 1 ==> c[i] == 2);
@*/
void backwardFull(int *a, int *b, int *c, int len) {
    /*@
    requires a != NULL;
    requires b != NULL;
    requires c != NULL;
    requires Perm(a[i], 1 \ 2);
    requires i == 0 ==> Perm(a[i], 1 \ 2);
    requires i < len - 1 ==> Perm(a[i + 1], 1 \ 2);
    requires Perm(b[i], 1 \ 2);
    requires Perm(c[i], write);
    requires i < len - 1 ==> a[i + 1] == 0;
    requires b[i] == i;
    ensures Perm(b[i], 1 \ 2);
    ensures Perm(c[i], write);
    ensures b[i] == i;
    ensures Perm(a[i], write);
    ensures a[i] == i + 1;
    ensures i < len - 1 ==> c[i] == 2;
    @*/
    for (int i = 0; i < len; i = i + 1) {
        /*@ recv ft; @*/
        a[i] = b[i] + 1;
        if (i < len - 1) {
            c[i] = a[i + 1] + 2;
        }
        /*@ send ft, 1: a != NULL ** 0 <= i ** i < len - 1 ** Perm(a[i + 1], 1 \ 2); @*/
    }
}