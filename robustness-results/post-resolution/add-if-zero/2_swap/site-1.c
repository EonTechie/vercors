/*@
decreases;
ensures \result >= 2;
pure int sizeof_int();
@*/

/*@
decreases;
ensures \result >= 2;
pure int sizeof_struct unknown_1216741689();
@*/


class IntBox {
    int value;
}

void swap(IntBox *pointer1, IntBox *pointer2) {
    int temp;
    temp = (*pointer1).value;
    (*pointer1).value = (*pointer2).value;
    if (0) {
        /*@ assert false; @*/
    } else {
        (*pointer2).value = temp;
    }
}