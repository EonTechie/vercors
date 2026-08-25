/*@
  context \pointer(sum, 1, write) **
          \pointer(remaining, 1, write) **
          \pointer(k, 1, 1\2);

  requires *remaining > 0;

  ensures *sum == \old(*sum) + \old(*k);
  ensures *remaining == \old(*remaining) - 1;
  ensures *k == \old(*k);
@*/

void func_aUQmIf1h(int *sum, int *remaining, int *k)
{
  *sum = *sum + *k;
  (*remaining)--;
}


/*@
  requires k >= 0 && m >= 0;
  ensures \result == k * m;
@*/
int mult_add(int k, int m)
{
  int sum = 0;
  int remaining = m;

  /*@ loop_invariant \pointer(&sum, 1, write) **
                    \pointer(&remaining, 1, write) **
                    \pointer(&k, 1, 1\2);
  loop_invariant 0 <= remaining && remaining <= m;
  loop_invariant sum == (m - remaining) * k;


  decreases remaining;

@*/
  while (0 < remaining)
  {
    func_aUQmIf1h(&sum, &remaining, &k);
  }

  return sum;
}