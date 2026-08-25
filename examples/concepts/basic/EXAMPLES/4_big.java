class big{

//@ context_everywhere arr != null;

//@ context_everywhere (\forall* int x; 0 <= x && x < arr.length; Perm(arr[x], read));

//@ requires arr.length > 0;

//@ ensures (\forall int x; 0 <= x && x < arr.length; \result >= arr[x]);
//@ ensures (\exists int x; 0 <= x && x < arr.length; \result == arr[x]);

static int isBiggest(int arr[]){

	int biggest = arr[0];

	//@ loop_invariant 1 <= i && i <= arr.length;
        //@ loop_invariant (\forall int x;   0 <= x && x < i;  biggest >= arr[x]);
        //@ loop_invariant (\exists int x; 0 <= x && x < i; biggest == arr[x]);


	for(int i = 1 ; i < arr.length ; i++){

		if(arr[i] > biggest){
		biggest = arr[i];

		}
	}
	return biggest;

}

}