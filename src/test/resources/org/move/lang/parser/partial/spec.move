module 0x1::M {
    fun m() {
        assume;
        let a = exists;
        1 ==> 2;
    }
    spec module {
        assert;
        assume;
        assume exists 1;

        ensures result ==> 1;
        ensures true
    }
}
