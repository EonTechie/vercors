void func_DPzkbq1I(int *i, int *sum, int **list)
{
  *sum = (*sum) + (*list)[*i];
}

int sum_list(int *list, int length)
{
  int sum = 0;
  int i = 0;
  for (; i < length; i++)
    func_DPzkbq1I(&i, &sum, &list);

  return sum;
}

