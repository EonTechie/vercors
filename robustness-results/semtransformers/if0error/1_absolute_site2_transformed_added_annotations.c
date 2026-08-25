/*@
  requires a != -2147483648;
  ensures \result == (a >= 0 ? a : -a);
@*/
int absolute(int a)
{
  if (a >= 0)
  {
    return a;
    if (0)
    {
      //@ assert false;
    }
    else
    {
    }
  }
  else
  {
    return -a;
  }
}

