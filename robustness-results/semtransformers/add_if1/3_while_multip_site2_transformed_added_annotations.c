/*@
  requires k >= 0 && m >= 0;
  ensures \result == k * m;
@*/
int mult_add(int k, int m)
{
  int sum = 0;
  int remaining = m;
      //@ loop_invariant 0 <= remaining && remaining <= m;
    //@ loop_invariant sum == (m - remaining) * k;
    //@ decreases remaining;
while (0 < remaining)
  {
    if (1)
    {
      sum = sum + k;
    }
    else
    {
    //@ assert false;
  }
    remaining--;
  }

  return sum;
}

