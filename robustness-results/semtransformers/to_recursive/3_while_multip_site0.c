void func_BQkTMOQc(int *sum, int *k, int *remaining)
{
  if (0 < (*remaining))
  {
    {
      *sum = (*sum) + (*k);
      (*remaining)--;
    }
    func_BQkTMOQc(sum, k, remaining);
  }
  else
  {
  }
}

int mult_add(int k, int m)
{
  int sum = 0;
  int remaining = m;
  func_BQkTMOQc(&sum, &k, &remaining);
  return sum;
}

