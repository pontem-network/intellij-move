module M {
    resource struct ValidatorConfig {
        val1: u8,
        val2: vector<u8>,
        operator_account: Option<address>,
        operator_account: Option<&signer>,
    }
}