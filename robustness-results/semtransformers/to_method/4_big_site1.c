void func_P2k9mvnK(int *biggest, int *i, int **arr)
{
  *biggest = (*arr)[*i];
}

int isBiggest(int *arr, int length)
{
  int biggest = arr[0];
  int i;
  for (i = 1; i < length; i++)
  {
    if (arr[i] > biggest)
    {
      func_P2k9mvnK(&biggest, &i, &arr);
    }
    else
    {
    }
  }

  return biggest;
}

