/*@
  requires length > 0;

  context_everywhere \pointer(arr, length, read);

  ensures
      (\forall int x;
          0 <= x && x < length;
          \result >= arr[x]);

  ensures
      (\exists int x;
          0 <= x && x < length;
          \result == arr[x]);
@*/
// for'un bütününü sarmalamış


int isBiggest(int *arr, int length)
{
  int biggest = arr[0];
  int i;



  if (1)
  {
        /*@
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

  }
  else
  {
    //@ assert false;
  }
  return biggest;
}

