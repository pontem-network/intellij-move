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
}