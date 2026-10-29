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
pure int sizeof_pointer_struct_unknown_367969175_();
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_struct_unknown_367969175();
@*/

struct Node {
    struct Node* next;
};

struct Node NodeT;

seq<struct Node*> Nodes;

/*@
decreases;
pure int sizeof_seq_pointer_struct_unknown_367969175__();
@*/

/*@
decreases;
pure int sizeof_struct_unknown_1565950627();
@*/

struct List {
    struct Node* head;
    
    seq<struct Node*> ns;
};

struct List ListT;

/*@
inline resource state(
    struct List *list
) =
    list != NULL ** Perm((*list).head, 1) ** Perm((*list).ns, 1) **
    ((*list).head == NULL
        ? |(*list).ns| == 0
        : |(*list).ns| > 0 ** (*list).ns[0] == (*list).head **
        (\forall* int i; i \in [0 .. |(*list).ns|] ==> {: (*list).ns[i] :} != NULL) **
        (\forall int i, int j;
            i \in [0 .. |(*list).ns|] && j \in [0 .. |(*list).ns|] && i != j ==>
                {: (*list).ns[i] :} != {: (*list).ns[j] :}
        ) **
        (\forall* int i; i \in [0 .. |(*list).ns|] ==> Perm({: (*(*list).ns[i]).next :}, 1)) **
        (\forall* int i, int j;
            i \in [0 .. |(*list).ns| - 1] && j == i + 1 ==> {: (*(*list).ns[i]).next :} == {: (*list).ns[j] :}
        ) **
        (*(*list).ns[|(*list).ns| - 1]).next == NULL);
@*/

/*@
yields int k;
requires state(l);
requires toremove != NULL;
requires toremove \in (*l).ns;
ensures state(l);
ensures |(*l).ns| == |\old((*l).ns)| - 1;
ensures k \in {0 .. |\old((*l).ns)|};
ensures toremove == \old((*l).ns[k]);
ensures (*l).ns[0..k] == \old((*l).ns[0..k]);
ensures (\forall int i, int j;
    i \in [k .. |\old((*l).ns)| - 1] && j == i + 1 ==> {: (*l).ns[i] :} == \old({: (*l).ns[j] :})
);
@*/
void removeBetter(struct List *l, struct Node *toremove) {
    struct Node **p;
    p = &(*l).head;
    k = 0;
    /*@ assert (*l).head != NULL; @*/
    /*@
    loop_invariant Perm(state(l), 1 \ 2);
    loop_invariant k \in {0 .. |(*l).ns|};
    loop_invariant p != NULL;
    loop_invariant k == 0 ==> p == &(*l).head;
    loop_invariant k != 0 ==> p == &(*(*l).ns[k - 1]).next;
    loop_invariant *p != NULL;
    loop_invariant toremove \in (*l).ns[k..];
    @*/
    while (*p != toremove) {
        /*@ assume \pointer_block(*p) == \pointer_block(toremove); @*/
        p = &(**p).next;
        k = k + 1;
        /*@ assert p == &(*(*l).ns[k - 1]).next; @*/
        if (1) {
            if (*p == NULL) {
                if (k != |(*l).ns| - 1) {
                    /*@ assert p == &(*(*l).ns[k - 1]).next; @*/
                    /*@ assert k - 1 < |(*l).ns| - 1; @*/
                    /*@ assert (*(*l).ns[k]).next == (*l).ns[k + 1]; @*/
                    /*@ assert *p != NULL; @*/
                    /*@ assert false; @*/
                }
                /*@ assert k + 1 == |(*l).ns| - 1; @*/
                /*@ assert false; @*/
            }
        } else {
            /*@ assert false; @*/
        }
    }
    /*@ assume \pointer_block(*p) == \pointer_block(toremove); @*/
    PRE:
    
    struct Node **oldP;
    oldP = p;
    seq<struct Node*> oldNs;
    oldNs = (*l).ns;
    *p = (*toremove).next;
    (*l).ns = (*l).ns[0..k] + (*l).ns[k + 1..];
    if ((*l).head == NULL) {
        /*@ assert oldP == &(*l).head; @*/
        /*@ assert \old[PRE]((*toremove).next == NULL); @*/
        /*@ assert \old[PRE]((*toremove).next == NULL); @*/
        if (|oldNs| > 1) {
            /*@ assert oldNs[1] == (*toremove).next; @*/
        }
        /*@ assert |oldNs| == 1; @*/
        /*@ assert |(*l).ns| == 0; @*/
    }
    /*@ assert state(l); @*/
    /*@ assert |(*l).ns| == |\old((*l).ns)| - 1; @*/
    /*@ assert k \in {0 .. |\old((*l).ns)|}; @*/
    /*@ assert toremove == \old((*l).ns[k]); @*/
    /*@ assert (*l).ns[0..k] == \old((*l).ns[0..k]); @*/
    /*@ assert (\forall int i, int j;
            i \in [k .. |\old((*l).ns)| - 1] && j == i + 1 ==> {: (*l).ns[i] :} == \old({: (*l).ns[j] :})
        ); @*/
}