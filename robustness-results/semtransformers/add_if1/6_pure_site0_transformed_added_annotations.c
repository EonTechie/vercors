/*@
  requires 0 <= n && n <= |xs|;
  decreases n;

  pure int prefixSum(seq<int> xs, int n) =
      n == 0
          ? 0
          : prefixSum(xs, n - 1) + xs[n - 1];
@*/

/*@
  given seq<int> xs;

  requires length >= 0;

  context_everywhere list != NULL;
  context_everywhere \pointer(list, length, read);
  context_everywhere |xs| == length;

  context_everywhere
      (\forall int x;
          0 <= x && x < length;
          xs[x] == list[x]);

  ensures \result == prefixSum(xs, length);
@*/
int sum_list(int *list, int length)
{
  int sum = 0;
  int i = 0;
  if (1)
  {
        /*@
      loop_invariant 0 <= i && i <= length;
      loop_invariant sum == prefixSum(xs, i);
      decreases length - i;
    @*/
for (; i < length; i++)
    {
      sum = sum + list[i];
    }

  }
  else
  {

  }
  return sum;
}

