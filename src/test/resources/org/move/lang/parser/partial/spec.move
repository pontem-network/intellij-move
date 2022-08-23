module 0x1::M {
    spec
    spec my_function
    spec MyStruct
    spec module
    spec fun
    spec fun myfun
    spec schema
    spec schema MySchema

    fun m() {
        assume;
        let a = exists;
        1 ==> 2;
    }
    spec module {
        include Schema

        assert;
        assume;
        assume exists 1;

        ensures result ==> 1;
        ensures true
    }
}
