struct IntBox
{
  int value;
};
/*@
  requires pointer1 != NULL;
  requires pointer2 != NULL;

  context pointer1 != pointer2 ==>
      Perm(pointer1->value, write) **
      Perm(pointer2->value, write);

  context pointer1 == pointer2 ==>
      Perm(pointer1->value, write);

  ensures pointer1->value == \old(pointer2->value);
  ensures pointer2->value == \old(pointer1->value);
@*/
static void swap(struct IntBox *pointer1, struct IntBox *pointer2)
{
  int temp = pointer1->value;
  pointer1->value = pointer2->value;
  if (1)
  {
    pointer2->value = temp;
  }
  else
  {
    //@ assert false;
  }
}

