module M {
    fun main() {
        spec {};
    }

    spec module {
        define all(): bool {}
        native define all2(): bool;
    }

    spec struct MyStruct {}
    spec fun myfun {}
    spec define spec_is_valid(addr: address) {}

    spec schema ModuleInvariant<X, Y> {}
}