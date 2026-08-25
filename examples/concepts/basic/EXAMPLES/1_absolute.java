// seed 

class Absolute {

	//@ ensures \result == (a >= 0 ? a : -a);

	static int absolute(int a)
{

		if (a>=0){
			return a;

		}else {
			
			return -a;
}




}}