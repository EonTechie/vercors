// for2while + to_recursive


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
        return sumRecursive(list, 0, 0);
    }




    /*@
      context_everywhere list != null;
      context_everywhere Perm(list[*], read);

      requires 0 <= i && i <= list.length;

      requires sum ==
          prefixSum(\values(list, 0, list.length), i);

      ensures \result ==
          prefixSum(\values(list, 0, list.length), list.length);

      decreases list.length - i;
    @*/


    static int sumRecursive(int[] list, int i, int sum) {

        if (i < list.length) {

            /*@
              assert 0 <= i + 1 && i + 1 <= list.length;

              assert prefixSum(\values(list, 0, list.length), i + 1)
                  == prefixSum(\values(list, 0, list.length), i)
                     + list[i];
            @*/

            return sumRecursive(
                list,
                i + 1,
                sum + list[i]
            );
        }

        return sum;
    }
}