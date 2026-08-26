struct IntBox {
    int value;
};

/*@
requires pointer1 != NULL;
requires pointer2 != NULL;
requires pointer1 != pointer2 ==> Perm((*pointer1).value, write) ** Perm((*pointer2).value, write);
requires pointer1 == pointer2 ==> Perm((*pointer1).value, write);
ensures pointer1 != pointer2 ==> Perm((*pointer1).value, write) ** Perm((*pointer2).value, write);
ensures pointer1 == pointer2 ==> Perm((*pointer1).value, write);
ensures (*pointer1).value == \old((*pointer2).value);
ensures (*pointer2).value == \old((*pointer1).value);
@*/
void swap(struct IntBox *pointer1, struct IntBox *pointer2) {
    int temp;
    if (1) {
        if (1) {
            if (1) {
                if (1) {
                    temp = (*pointer1).value;
                } else {
                    /*@ assert false; @*/
                }
            } else {
                /*@ assert false; @*/
            }
        } else {
            /*@ assert false; @*/
        }
    } else {
        /*@ assert false; @*/
    }
    if (1) {
        (*pointer1).value = (*pointer2).value;
    } else {
        /*@ assert false; @*/
    }
    (*pointer2).value = temp;
}