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
    sum = sum + k;
    remaining--;
  }

  return sum;
  if (0)
  {
    //@ assert false;
  }
  else
  {
  }
}

