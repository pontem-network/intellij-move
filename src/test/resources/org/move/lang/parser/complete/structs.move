module M {
    struct ValidatorConfig<T> {
        val1: u8,
        val2: vector<T>,
    }

    resource struct ResourceValidatorConfig<T> {
        val1: u8,
        val2: vector<T>,
        operator_account: Option<address>,
        operator_account: Option<&signer>,
    }

    fun main() {
        let a = Struct { a: val, b: 1 + 1 };
        let a = Struct { a: val, b: Struct2 { val, anotherval: 1 + 1 } };
    }
}