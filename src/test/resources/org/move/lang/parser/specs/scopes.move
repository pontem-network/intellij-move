module M {
    fun main() {
        spec {};
        spec {}
    }

    spec module {
        fun all(): bool {}
        fun uninterpreted(addr: address);

        // Native function which is defined in the prover's prelude.
        native fun serialize<MoveValue>(v: &MoveValue): vector<u8>;
    }

    spec MyStruct {}
    spec fun spec_is_valid(addr: address) {}
    spec fun spec_uninterpreted(addr: address);
    spec native fun spec_is_valid_native(addr: address);

    fun unpack() {}
    spec unpack {}

    spec schema ModuleInvariant<X, Y> {}
}
