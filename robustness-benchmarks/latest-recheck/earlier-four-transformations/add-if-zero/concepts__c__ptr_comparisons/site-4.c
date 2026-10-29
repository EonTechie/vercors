void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

int IntptrT;

int UintptrT;

int Int8T;

int Int16T;

int Int32T;

int Int64T;

int Uint8T;

int Uint16T;

int Uint32T;

int Uint64T;

/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
requires x != NULL;
requires Perm(*x, write);
ensures Perm(*x, write);
@*/
void bar(int *x);

void foo() {
    int *a;
    a = (int*) vercorsMallocNonNull(sizeof(int) * 4);
    /*@ assume a[0] == 1; @*/
    /*@ assume a[1] == 2; @*/
    /*@ assume a[2] == 3; @*/
    /*@ assume a[3] == 4; @*/
    int *start;
    start = &a[0];
    int *p;
    p = start;
    int *end;
    end = &a[3];
    /*@ assert end >= start; @*/
    /*@ assert start < end; @*/
    /*@
    loop_invariant start <= p;
    loop_invariant p <= end;
    loop_invariant (\forall* int i; i \in [0 .. 4] ==> Perm(a[i], write));
    @*/
    while (p <= end) {
        if (0) {
            /*@ assert false; @*/
        } else {
            if (p == end) {
                break;
            } else {
                /*@ assert end > p; @*/
            }
        }
    }
}