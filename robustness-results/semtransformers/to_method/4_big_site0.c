/*@
  context \pointer(biggest, 1, write) **
          \pointer(i, 1, 1\2) **
          \pointer(arr, 1, 1\2);

  requires **arr !=NULL;

      loop_invariant 1 <= i && i <= length;

      loop_invariant
          (\forall int x;
              0 <= x && x < i;
              biggest >= arr[x]);

      loop_invariant
          (\exists int x;
              0 <= x && x < i;
              biggest == arr[x]);

@*/


void func_A3vt1a3M(int *biggest, int *i, int **arr)
{
  if ((*arr)[*i] > (*biggest))
  {
    *biggest = (*arr)[*i];
  }
  else
  {
  }
}

int isBiggest(int *arr, int length)
{
  int biggest = arr[0];
  int i;
  for (i = 1; i < length; i++)
    func_A3vt1a3M(&biggest, &i, &arr);

  return biggest;
}

