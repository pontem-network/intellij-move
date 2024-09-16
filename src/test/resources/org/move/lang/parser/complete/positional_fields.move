module 0x1::positional_fields {
    struct S1(u8);
    struct S2<T>(u8, bool, aptos_framework::option::Option<T>);

    struct S0() has copy;
    struct S0 { val: u8 } has copy;

    enum E<T> {
        V(bool, aptos_framework::option::Option<T>)
    }

    // value construction with anonymous fields
    fun construct() {
        let x = S(42);
        let y = E::V(true, 42);
    }

    // value destruction with anonymous fields
    fun destruct(x: S, y: E) {
        x.0;
        let S(_x) = x;
        match (y) {
            E::V(_x, _y) => {},
        }
    }
}
