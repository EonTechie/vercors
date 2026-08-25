//@ ensures \result == (a >= 0 ? a : -a);

int absolute(int a) {
    if (a >= 0) {
        return a;
    } else {
        return -a;
    }
}