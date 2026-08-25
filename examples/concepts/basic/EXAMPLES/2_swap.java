class IntBox {
    int value;
}

class Swap {

    /*@
      requires pointer1 != null;
      requires pointer2 != null;

      context pointer1 != pointer2 ==>
          Perm(pointer1.value, write) **
          Perm(pointer2.value, write);

      context pointer1 == pointer2 ==>
          Perm(pointer1.value, write);

      ensures pointer1.value == \old(pointer2.value);
      ensures pointer2.value == \old(pointer1.value);
    @*/
    static void swap(IntBox pointer1, IntBox pointer2) {
        int temp = pointer1.value;
        pointer1.value = pointer2.value;
        pointer2.value = temp;
    }
}