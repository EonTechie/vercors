void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires expression;
@*/
void assert(_Bool expression);

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
decreases;
ensures \result >= 4;
pure int sizeof_struct_unknown_1442252018();
@*/

struct Point {
    int x;
    
    int y;
};

/*@
decreases;
ensures \result >= 12;
pure int sizeof_struct_unknown_676627074();
@*/

struct Triangle {
    struct Point p1;
    
    struct Point p2;
    
    struct Point p3;
};

/*@
decreases;
ensures \result >= 2;
pure int sizeof_pointer_struct_unknown_1442252018_();
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_struct_unknown_1871777911();
@*/

struct Polygon {
    struct Point* ps;
};

/*@
decreases;
ensures \result >= 2;
pure int sizeof_pointer_struct_unknown_1667096469_();
@*/

/*@
decreases;
ensures \result >= 4;
pure int sizeof_struct_unknown_1667096469();
@*/

struct LinkedList {
    struct LinkedList* p1;
    
    int x;
};

/*@
requires p != NULL;
requires Perm((*p).x, write);
requires Perm((*p).y, write);
ensures p != NULL;
ensures Perm((*p).x, write);
ensures Perm((*p).y, write);
ensures (*p).x == 0;
ensures (*p).y == 0;
ensures \old(*p) == *p;
@*/
void alterStruct(struct Point *p) {
    (*p).x = 0;
    (*p).y = 0;
}

/*@
requires p != NULL;
requires Perm((*p).x, write);
requires Perm((*p).y, write);
ensures p != NULL;
ensures Perm((*p).x, write);
ensures Perm((*p).y, write);
ensures (*p).x == 0;
ensures (*p).y == 0;
ensures \old(*p) == *p;
@*/
void alterStruct2(struct Point *p) {
    (*p).x = 0;
    (*p).y = 0;
}

/*@
requires p != NULL;
requires Perm(*p, write);
ensures p != NULL;
ensures Perm(*p, write);
ensures (*p).x == \old((*p).x + 1);
ensures (*p).y == \old((*p).y + 1);
ensures \old(*p) == *p;
@*/
void alterStruct1(struct Point *p) {
    (*p).x = (*p).x + 1;
    (*p).y = (*p).y + 1;
}

/*@
requires Perm(p.x, 1 \ 1);
requires Perm(p.y, 1 \ 1);
ensures Perm(p.x, 1 \ 1);
ensures Perm(p.y, 1 \ 1);
@*/
void alterCopyStruct(struct Point p) {
    p.x = 0;
    p.y = 0;
}

/*@
requires Perm(p, 1 \ 1);
ensures Perm(p, 1 \ 1);
@*/
void alterCopyStruct2(struct Point p) {
    p.x = 0;
    p.y = 0;
}

/*@
requires r != NULL;
requires Perm(*r, 1 \ 2);
ensures r != NULL;
ensures Perm(*r, 1 \ 2);
ensures \result == ((*r).p1.x + (*r).p2.x + (*r).p3.x) / 3;
@*/
int avrX(struct Triangle *r) {
    return ((*r).p1.x + (*r).p2.x + (*r).p3.x) / 3;
}

/*@
requires n >= 0;
requires inp != NULL;
requires \pointer_length(inp) >= n;
requires (\forall int i, int j; 0 <= i && i < n && 0 <= j && j < n ==> i != j ==> {: inp[i] :} != {: inp[j] :});
requires (\forall* int i; 0 <= i && i < n ==> Perm(inp[i].x, 1 \ 10));
decreases assume;
ensures |\result| == n;
ensures (\forall int i; 0 <= i && i < n ==> \result[i] == inp[i].x);
pure seq<int> inpToSeq(struct Point *inp, int n) = n == 0 ? seq<int>{} : inpToSeq(inp, n - 1) + [inp[n - 1].x];
@*/

/*@
decreases |xs|;
ensures |xs| == 0 ==> \result == 0;
ensures |xs| > 0 ==> \result == sumSeq(xs[..|xs| - 1]) + xs[|xs| - 1];
pure int sumSeq(seq<int> xs) = |xs| == 0 ? 0 : sumSeq(xs[..|xs| - 1]) + xs[|xs| - 1];
@*/

/*@
requires len > 0;
requires p != NULL;
requires Perm(*p, 1 \ 2);
requires (*p).ps != NULL;
requires \pointer_length((*p).ps) >= len;
requires (\forall int i, int j;
    0 <= i && i < len && 0 <= j && j < len ==> i != j ==> {: (*p).ps[i] :} != {: (*p).ps[j] :}
);
requires (\forall* int i; 0 <= i && i < len ==> Perm((*p).ps[i], 1 \ 2));
ensures p != NULL;
ensures Perm(*p, 1 \ 2);
ensures (*p).ps != NULL;
ensures \pointer_length((*p).ps) >= len;
ensures (\forall int i, int j;
    0 <= i && i < len && 0 <= j && j < len ==> i != j ==> {: (*p).ps[i] :} != {: (*p).ps[j] :}
);
ensures (\forall* int i; 0 <= i && i < len ==> Perm((*p).ps[i], 1 \ 2));
ensures len == 3 ==> \result == ((*p).ps[0].x + (*p).ps[1].x + (*p).ps[2].x) / len;
@*/
int avrXPol(struct Polygon *p, int len) {
    int sum;
    sum = 0;
    /*@
    loop_invariant 0 <= i;
    loop_invariant i <= len;
    loop_invariant p != NULL;
    loop_invariant Perm(*p, 1 \ 2);
    loop_invariant (*p).ps != NULL;
    loop_invariant \pointer_length((*p).ps) >= len;
    loop_invariant (\forall int i1, int j;
        0 <= i1 && i1 < len && 0 <= j && j < len ==> i1 != j ==> {: (*p).ps[i1] :} != {: (*p).ps[j] :}
    );
    loop_invariant (\forall* int i1; 0 <= i1 && i1 < len ==> Perm((*p).ps[i1], 1 \ 2));
    loop_invariant i == 0 ==> sum == 0;
    loop_invariant i == 1 ==> sum == (*p).ps[0].x;
    loop_invariant i == 2 ==> sum == (*p).ps[0].x + (*p).ps[1].x;
    loop_invariant i == 3 ==> sum == (*p).ps[0].x + (*p).ps[1].x + (*p).ps[2].x;
    @*/
    for (int i = 0; i < len; i = i + 1) {
        sum = sum + (*p).ps[i].x;
    }
    return sum / len;
}

int main() {
    struct Point *P_213212403;
    struct Point *pp;
    pp = &*P_213212403;
    assert(pp != NULL);
    (*P_213212403).x = 1;
    (*P_213212403).y = 2;
    assert((*pp).x == 1);
    assert((*pp).y == 2);
    alterCopyStruct(*P_213212403);
    assert((*P_213212403).x == 1);
    assert((*P_213212403).y == 2);
    alterStruct(pp);
    assert((*pp).x == 0);
    assert((*P_213212403).x == 0);
    alterStruct1(pp);
    assert((*P_213212403).x == 1 && (*P_213212403).y == 1);
    struct Point *P1_1617192987;
    struct Point *P2_102728008;
    struct Point *P3_543885244;
    (*P1_1617192987).x = 1;
    (*P1_1617192987).y = 1;
    (*P2_102728008).x = 2;
    if (1) {
        (*P1_1617192987).y = 2;
    } else {
        /*@ assert false; @*/
    }
    (*P3_543885244).x = 3;
    (*P1_1617192987).y = 3;
    struct Triangle *R_2071177188;
    struct Triangle *rr;
    rr = &*R_2071177188;
    (*R_2071177188).p1 = *P1_1617192987;
    (*R_2071177188).p2 = *P2_102728008;
    (*R_2071177188).p3 = *P3_543885244;
    assert(avrX(rr) == 2);
    struct Point *ps;
    ps = (struct Point*) vercorsMalloc(sizeof(struct Point) * 3);
    /*@ assume ps[0] == *P1_1617192987; @*/
    /*@ assume ps[1] == *P2_102728008; @*/
    /*@ assume ps[2] == *P3_543885244; @*/
    struct Polygon *Pol_987055806;
    struct Polygon *ppols;
    ppols = &*Pol_987055806;
    (*Pol_987055806).ps = ps;
    int avrPol;
    avrPol = avrXPol(ppols, 3);
    assert(avrPol == 2);
    return 0;
}