/*
Seed: Absolute

Transformation categories used: 4

1. Dead error insertion
   Applications: 1

2. IF encapsulation
   Applications: 1

3. Loop insertion
   Applications: 1
   Manual adaptation; this is not exact loop deepening because
   the seed program did not contain an existing loop.

4. Array indirection
   Applications: 2
   Manual adaptation.
*/

class Box {

    int value;

    //@ ensures Perm(this.value, write);
    Box() {
    }
}

class Absolute {

    //@ ensures \result == (a >= 0 ? a : -a);
    static int absolute(int a) {

        Box p = new Box();

        if (false) {
            //@ assert false;
        } else {

            if (true) {

                //@ loop_invariant Perm(p.value, write);
                while (true) {

                    if (a >= 0) {
                        int[] arr = new int[1];
                        arr[0] = a;
                        p.value = arr[0];
                        return p.value;
                    } else {
                        int[] temp = new int[1];
                        temp[0] = a;
                        temp[0] = -1 * temp[0];
                        p.value = temp[0];
                        return p.value;
                    }
                }
            }
        }

        return 0;
    }
}