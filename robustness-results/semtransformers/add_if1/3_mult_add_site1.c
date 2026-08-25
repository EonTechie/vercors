int mult_add(int k, int m)
{
  if ((m == 0) || (k == 0))
  {
    if (1)
    {
      return 0;
    }
    else
    {
    }
  }
  else
  {
    return m + mult_add(k - 1, m);
  }
}

