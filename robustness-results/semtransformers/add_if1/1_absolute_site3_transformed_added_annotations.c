/*@
  requires a != -2147483648;
  ensures \result == (a >= 0 ? a : -a);
@*/
int absolute(int a)
{
  if (a >= 0)
  {
    if (1)
    {
      return a;
    }
    else
    {
    //@ assert false;
  }
  }
  else
  {
    return -a;
  }
}

