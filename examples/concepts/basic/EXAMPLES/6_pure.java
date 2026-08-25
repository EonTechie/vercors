

// |xs| diye bir length alma işlemiva rmı seq'lerin length 'i böyle mi alınıyor bu vercors spec syntax'i midir?



/*@
  requires 0 <= n && n <= |xs|; 
  decreases n;

  pure int prefixSum(seq<int> xs, int n) =
      n == 0
          ? 0
          : prefixSum(xs, n - 1) + xs[n - 1];
@*/

class Sum {





    /*@
      context_everywhere list != null;
      context_everywhere Perm(list[*], read);

      ensures \result ==
          prefixSum(\values(list, 0, list.length), list.length);
    @*/



    static int sum_list(int[] list) {

        int sum = 0;
        int i = 0;

	    /*@
          loop_invariant 0 <= i && i <= list.length;

          loop_invariant sum ==
              prefixSum(\values(list, 0, list.length), i);

          decreases list.length - i;
        @*/

        for (; i < list.length; i++) {
            sum = sum + list[i];
        }

        return sum;
    }
}