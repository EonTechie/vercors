int isBiggest(int *arr, int length)
{
  int biggest = arr[0];
  int i;
  for (i = 1; i < length; i++)
  {
    if (arr[i] > biggest)
    {
      if (0)
      {
        //@ assert false;
      }
      else
      {
      }
      biggest = arr[i];
    }
    else
    {
    }
  }

  return biggest;
}

