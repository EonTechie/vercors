
class Absolute {

//@ requires a >= 0;
//@ ensures \result == a;
static int positive(int a){
	if (a>=0){
return a;
}
}


//@ requires a < 0;
//@ ensures \result == -a ;
static int negative(int a){
	if (a<0){
return -a;
}
}


	//@ ensures \result == (a >= 0 ? a : -a);

	static int absolute(int a)
{

	if (a>=0){
return positive(a);
}
	else{
return negative(a);
}



}}