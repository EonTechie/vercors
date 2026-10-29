void *vercorsMallocNonNull(int size);
void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires expression;
@*/
void assert(_Bool expression);

const double Nan = vercorsNAN;

const double Infinity = vercorsINFINITY;

/*@
decreases;
pure double mPi() = 3.14159265358979323846;
@*/

/*@
decreases;
ensures \result == (x >= 0 ? x : -x);
@*/
/*@ pure @*/ float fabsf(float x);

/*@
requires x >= 0;
decreases;
ensures \result == \pow(x, 0.5);
@*/
/*@ pure @*/ float sqrtf(float x);

/*@
decreases;
ensures \result >= -1;
ensures \result <= 1;
@*/
/*@ pure @*/ float sinf(float x);

/*@
requires x >= -1;
requires x <= 1;
decreases;
ensures \result >= -mPi() / 2;
ensures \result <= mPi() / 2;
@*/
/*@ pure @*/ float asinf(float x);

/*@
decreases;
ensures \result >= -1;
ensures \result <= 1;
@*/
/*@ pure @*/ float cosf(float x);

/*@
requires x >= -1;
requires x <= 1;
decreases;
ensures \result >= 0;
ensures \result <= mPi();
@*/
/*@ pure @*/ float acosf(float x);

/*@
decreases;
ensures x == 0 ==> \result == 0;
@*/
/*@ pure @*/ float tanf(float x);

/*@
decreases;
ensures \result >= -mPi() / 2;
ensures \result <= mPi() / 2;
@*/
/*@ pure @*/ float atanf(float x);

/*@
decreases;
ensures -mPi() <= \result;
ensures \result <= mPi();
@*/
/*@ pure @*/ float atan2f(float x, float y);

/*@
decreases;
ensures \result == (expf(x) - expf(-x)) \ 2;
@*/
/*@ pure @*/ float sinhf(float x);

/*@
decreases;
ensures \result == (expf(x) + expf(-x)) \ 2;
@*/
/*@ pure @*/ float coshf(float x);

/*@
decreases;
ensures \result == (expf(x) - expf(-x)) \ (expf(x) + expf(-x));
@*/
/*@ pure @*/ float tanhf(float x);

/*@
decreases;
ensures \result == powf(x * x + y * y, 0.5);
@*/
/*@ pure @*/ float hypotf(float x, float y);

/*@
decreases;
ensures \result == powf(2.7182818284, x);
@*/
/*@ pure @*/ float expf(float x);

/*@
decreases;
@*/
/*@ pure @*/ float logf(float x);

/*@
decreases;
ensures \result == \pow(x, y);
ensures x > 0 ==> \result > 0;
@*/
/*@ pure @*/ float powf(float x, float y);

/*@
decreases;
ensures !(\is_int(x)) ==> \result == (float)(int)x;
ensures !(\is_int(x)) ==> (int)\result == (int)x;
ensures (\is_int(x)) ==> \result == x;
@*/
/*@ pure @*/ float floorf(float x);

/*@
decreases;
ensures \result == ((\is_int(x)) ? x : (float)((int)x + 1));
ensures !(\is_int(x)) ==> (int)\result == (int)x + 1;
@*/
/*@ pure @*/ float ceilf(float x);

/*@
decreases;
ensures !(x < 0 && (\is_int(x - 0.5))) ==> \result == (float)(int)(x + 0.5);
ensures !(x < 0 && (\is_int(x - 0.5))) ==> (int)\result == (int)(x + 0.5);
ensures x < 0 && (\is_int(x - 0.5)) ==> \result == x - 0.5;
@*/
/*@ pure @*/ float roundf(float x);

/*@
decreases;
ensures \result == (x >= 0 ? x : -x);
@*/
/*@ pure @*/ double fabs(double x);

/*@
requires x >= 0;
decreases;
ensures \result == pow(x, 0.5);
@*/
/*@ pure @*/ double sqrt(double x);

/*@
decreases;
ensures \result >= -1;
ensures \result <= 1;
@*/
/*@ pure @*/ double sin(double x);

/*@
requires x >= -1;
requires x <= 1;
decreases;
ensures \result >= -mPi() / 2;
ensures \result <= mPi() / 2;
@*/
/*@ pure @*/ double asin(double x);

/*@
decreases;
ensures \result >= -1;
ensures \result <= 1;
@*/
/*@ pure @*/ double cos(double x);

/*@
requires x >= -1;
requires x <= 1;
decreases;
ensures \result >= 0;
ensures \result <= mPi();
@*/
/*@ pure @*/ double acos(double x);

/*@
decreases;
ensures x == 0 ==> \result == 0;
@*/
/*@ pure @*/ double tan(double x);

/*@
decreases;
ensures \result >= -mPi() / 2;
ensures \result <= mPi() / 2;
@*/
/*@ pure @*/ double atan(double x);

/*@
decreases;
ensures -mPi() <= \result;
ensures \result <= mPi();
@*/
/*@ pure @*/ double atan2(double x, double y);

/*@
decreases;
@*/
/*@ pure @*/ double sinh(double x);

/*@
decreases;
ensures \result == (exp(x) + exp(-x)) \ 2;
@*/
/*@ pure @*/ double cosh(double x);

/*@
decreases;
ensures \result == (exp(x) - exp(-x)) \ (exp(x) + exp(-x));
@*/
/*@ pure @*/ double tanh(double x);

/*@
decreases;
ensures \result == pow(x * x + y * y, 0.5);
@*/
/*@ pure @*/ double hypot(double x, double y);

/*@
decreases;
ensures \result == pow(2.7182818284, x);
@*/
/*@ pure @*/ double exp(double x);

/*@
decreases;
@*/
/*@ pure @*/ double log(double x);

/*@
decreases;
ensures \result == \pow(x, y);
ensures x > 0 ==> \result > 0;
@*/
/*@ pure @*/ double pow(double x, double y);

/*@
decreases;
ensures \result == (double)(int)x;
ensures !(\is_int(x)) ==> (int)\result == (int)x;
ensures (\is_int(x)) ==> \result == x;
@*/
/*@ pure @*/ double floor(double x);

/*@
decreases;
ensures \result == ((\is_int(x)) ? x : (double)((int)x + 1));
ensures !(\is_int(x)) ==> (int)\result == (int)x + 1;
@*/
/*@ pure @*/ double ceil(double x);

/*@
decreases;
ensures !(x < 0 && (\is_int(x - 0.5))) ==> \result == (double)(int)(x + 0.5);
ensures !(x < 0 && (\is_int(x - 0.5))) ==> (int)\result == (int)(x + 0.5);
ensures x < 0 && (\is_int(x - 0.5)) ==> \result == x - 0.5;
@*/
/*@ pure @*/ double round(double x);

int main() {
    /*@ assert ceil(1.5) == 2.0; @*/
    /*@ assert ceil(2.0) == 2.0; @*/
    /*@ assert ceil(-1.5) == -1.0; @*/
    /*@ assert (int)ceil(1.5) == 2; @*/
    /*@ assert (int)ceil(2.0) == 2; @*/
    /*@ assert (int)ceil(-1.5) == -1; @*/
    /*@ assert floor(1.5) == 1.0; @*/
    /*@ assert floor(2.0) == 2.0; @*/
    /*@ assert floor(-1.5) == -2.0; @*/
    /*@ assert (int)floor(1.5) == 1; @*/
    /*@ assert (int)floor(2.0) == 2; @*/
    /*@ assert (int)floor(-1.5) == -2; @*/
    if (0) {
        /*@ assert false; @*/
    } else {
        /*@ assert round(1.5) == 2.0; @*/
    }
    /*@ assert round(1.4) == 1.0; @*/
    /*@ assert round(2.0) == 2.0; @*/
    /*@ assert round(-1.5) == -2.0; @*/
    /*@ assert round(-1.4) == -1.0; @*/
    /*@ assert (int)round(1.5) == 2; @*/
    /*@ assert (int)round(1.4) == 1; @*/
    /*@ assert (int)round(2.0) == 2; @*/
    /*@ assert (int)round(-1.5) == -2; @*/
    /*@ assert (int)round(-1.4) == -1; @*/
    /*@ assert fabs(-1.5) == 1.5; @*/
    /*@ assert fabs(1.5) == 1.5; @*/
    /*@ assert pow(2.0, 2.0) == 4.0; @*/
    /*@ assert pow(2.0, 0.0) == 1.0; @*/
    /*@ assert pow(4.0, 0.5) == 2.0; @*/
    /*@ assert sqrt(4.0) == 2.0; @*/
}