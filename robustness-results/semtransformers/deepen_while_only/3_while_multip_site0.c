extern boolean __VERIFIER_nondet_int();

int mult_add(int k, int m)
{
  int sum = 0;
  int remaining = m;


    //@ loop_invariant 0 <= remaining && remaining <= m;
    //@ loop_invariant sum == (m - remaining) * k;
    //@ decreases remaining;
  while (0 < remaining)


    //@ loop_invariant 0 <= remaining && remaining <= m;
    //@ loop_invariant sum == (m - remaining) * k;
    //@ decreases remaining;
    while (__VERIFIER_nondet_int() && (0 < remaining))
  {
    sum = sum + k;
    remaining--;
  }


  return sum;
}

