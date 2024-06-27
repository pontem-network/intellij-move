module 0x1::function_parameters {
    fun prologue(
    a: u8,
    b: u8,
    c: u8,
    ): u8 {}

    spec prologue(
    a: u8,
    b: u8,
    c: u8,
    ): u8 {}

    spec fun prologue(
    a: u8,
    b: u8,
    c: u8,
    ): u8 {}

    spec module {
        fun prologue(
        a: u8,
        b: u8,
        c: u8,
        ): u8 {}
    }
}
