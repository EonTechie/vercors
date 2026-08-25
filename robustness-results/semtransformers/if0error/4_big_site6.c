int isBiggest(int *arr, int length)
{
  int biggest = arr[0];
  int i;
  for (i = 1; i < length; i++)
  {
    if (arr[i] > biggest)
    {
      biggest = arr[i];
    }
    else
    {
    }
    if (0)
    {
      //@ assert false;
    }
    else
    {
    }
  }

  return biggest;
}

