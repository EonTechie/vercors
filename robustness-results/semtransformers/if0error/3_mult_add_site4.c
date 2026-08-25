int mult_add(int k, int m)
{
  if ((m == 0) || (k == 0))
  {
    return 0;
  }
  else
  {
    return m + mult_add(k - 1, m);
    if (0)
    {
      //@ assert false;
    }
    else
    {
    }
  }
}

