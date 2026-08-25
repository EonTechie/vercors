int isBiggest(int *arr, int length)
{
  int biggest = arr[0];
  if (0)
  {
    //@ assert false;
  }
  else
  {
  }
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
  }

  return biggest;
}

