module 0x1::enums {
    enum Color has copy, drop {
        RGB{red: u64, green: u64, blue: u64},
        Red,
        Blue,
    }

    enum ColorUsesBlockNoComma {
        RGB{red: u64, green: u64, blue: u64}
        Red,
        Blue,
    } has copy;

    enum CommonFields {
        Foo{x: u64, y: u8},
        Bar{x: u64, z: u32}
    }

    enum Outer {
        None,
        One{i: Inner},
        Two{i: Inner, b: Box},
    }

    fun main(one: 0x1::m::S::One, two: m::S::Two) {}
}
