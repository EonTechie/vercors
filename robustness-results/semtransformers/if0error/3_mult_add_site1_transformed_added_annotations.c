/*@
  requires k >= 0 && m >= 0;
  ensures \result == k * m;
  decreases k;
@*/
int mult_add(int k, int m)
{
  if ((m == 0) || (k == 0))
  {
    if (0)
    {
      //@ assert false;
    }
    else
    {
    }
    return 0;
  }
  else
  {
    return m + mult_add(k - 1, m);
  }
}

