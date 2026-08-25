struct IntBox
{
  int value;
};
static void swap(struct IntBox *pointer1, struct IntBox *pointer2)
{
  int temp = pointer1->value;
  if (1)
  {
    pointer1->value = pointer2->value;
  }
  else
  {
  }
  pointer2->value = temp;
}

