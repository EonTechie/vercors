void *vercorsMalloc(int size);
void vercorsFree(void *ptr);

/*@
requires expression;
@*/
void assert(_Bool expression);

/*@
requires y != 0;
ensures \result == x / y;
@*/
int divEucl(int x, int y) {
    int q;
    q = x / y;
    int r;
    r = x % y;
    return r < 0 ? q + (y > 0 ? -1 : 1) : q;
}

/*@
requires y != 0;
ensures \result == x % y;
@*/
int modEucl(int x, int y) {
    int r;
    r = x % y;
    return r < 0 ? r + (y > 0 ? y : -y) : r;
}

int main() {
    assert(8 / 3 == 2);
    assert(8 % 3 == 2);
    assert(8 / -3 == -2);
    assert(8 % -3 == 2);
    assert(-8 / 3 == -2);
    assert(-8 % 3 == -2);
    assert(-8 / -3 == 2);
    assert(-8 % -3 == -2);
    assert(1 / 2 == 0);
    assert(1 % 2 == 1);
    assert(1 / -2 == 0);
    assert(1 % -2 == 1);
    assert(-1 / 2 == 0);
    assert(-1 % 2 == -1);
    assert(-1 / -2 == 0);
    assert(-1 % -2 == -1);
    if (0) {
        /*@ assert false; @*/
    } else {
        assert(divEucl(8, 3) == 2);
    }
    /*@ assert 8 / 3 == 2; @*/
    assert(modEucl(8, 3) == 2);
    /*@ assert 8 % 3 == 2; @*/
    assert(divEucl(8, -3) == -2);
    /*@ assert 8 / -3 == -2; @*/
    assert(modEucl(8, -3) == 2);
    /*@ assert 8 % -3 == 2; @*/
    assert(divEucl(-8, 3) == -3);
    /*@ assert -8 / 3 == -3; @*/
    assert(modEucl(-8, 3) == 1);
    /*@ assert -8 % 3 == 1; @*/
    assert(divEucl(-8, -3) == 3);
    /*@ assert -8 / -3 == 3; @*/
    assert(modEucl(-8, -3) == 1);
    /*@ assert -8 % -3 == 1; @*/
    assert(divEucl(1, 2) == 0);
    /*@ assert 1 / 2 == 0; @*/
    assert(modEucl(1, 2) == 1);
    /*@ assert 1 % 2 == 1; @*/
    assert(divEucl(1, -2) == 0);
    /*@ assert 1 / -2 == 0; @*/
    assert(modEucl(1, -2) == 1);
    /*@ assert 1 % -2 == 1; @*/
    assert(divEucl(-1, 2) == -1);
    /*@ assert -1 / 2 == -1; @*/
    assert(modEucl(-1, 2) == 1);
    /*@ assert -1 % 2 == 1; @*/
    assert(divEucl(-1, -2) == 1);
    /*@ assert -1 / -2 == 1; @*/
    assert(modEucl(-1, -2) == 1);
    /*@ assert -1 % -2 == 1; @*/
    return 0;
}