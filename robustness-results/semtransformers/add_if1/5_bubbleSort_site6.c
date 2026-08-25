void bubbleSort(int arr[], int length)
{
  int n = length;
  for (int i = 0; i < (n - 1); i++)
  {
    for (int j = 0; j < ((n - 1) - i); j++)
    {
      if (arr[j] > arr[j + 1])
      {
        int temp = arr[j];
        if (1)
        {
          arr[j] = arr[j + 1];
        }
        else
        {
        }
        arr[j + 1] = temp;
      }
      else
      {
      }
    }

  }

}

