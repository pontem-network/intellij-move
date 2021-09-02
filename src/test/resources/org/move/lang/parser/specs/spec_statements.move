module M {
    fun main() {
        let apply = 1;
        let emits = 1;
        let to = 1;
        let except = 1;
    }
    spec main {
        fun inner_spec_fun() {}
        emits msg to handle if !is_synthetic;
        apply ModuleInvariant to bar;
        aborts_if exists(Signer::spec_address_of(to_limit));
        aborts_if exists<Window>(Signer::spec_address_of(to_limit));
    }
}
