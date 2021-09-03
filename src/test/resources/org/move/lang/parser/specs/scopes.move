module M {
    fun main() {
        spec {};
    }

    spec module {
        fun all(): bool {}

        /// Native function which is defined in the prover's prelude.
        native fun serialize<MoveValue>(v: &MoveValue): vector<u8>;
    }

    spec MyStruct {}
    spec fun spec_is_valid(addr: address) {}
    spec native fun spec_is_valid_native(addr: address);

    fun unpack() {}
    spec unpack {}

    spec schema ModuleInvariant<X, Y> {}
}
