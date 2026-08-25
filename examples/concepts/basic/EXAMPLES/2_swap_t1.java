class IntBox {
    int value;
}

class Swap {


        //@ requires box != null;
        //@ context Perm(box.value, 1\2);
  	//@ ensures \result == \old(box.value);
	//@  ensures box.value == \old(box.value);

	static int assign(IntBox box){
		return box.value;


}



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
        int temp = assign(pointer1);
        pointer1.value = assign(pointer2);
        pointer2.value = temp;
    }
}