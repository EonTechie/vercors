class Box{
	int value;

	//@ ensures Perm(this.value, write);
	Box(){}
}

// loop deepening, if encapsulation, dead errorr, array indirection, 
class Absolute {



	//@ ensures \result == (a >= 0 ? a : -a);
        
	static int absolute(int a)
{
	Box p = new Box();
	

if(false){
	//@ assert false;
}else{

	if(true){
	    //@ loop_invariant Perm(p.value, write);
	    while(true)
            {
		if (a>=0){
			int[] arr = new int[1];
			arr[0] = a;
			p.value = a;
			return p.value;			;

		}else {
			int[] temp = new int[1];
			temp[0] = a;
			temp[0] = -1*temp[0];
			p.value = a;
			p.value = -1*p.value;
			return p.value;
	    }

	}

}

}

}


}