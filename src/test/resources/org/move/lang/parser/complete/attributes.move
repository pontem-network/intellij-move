#[test]
module 0x1::M {
    #[test_only]
    use 0x1::SomeOtherModule;

    #[test_only]
    const MY_CONST: u8 = 1;

    #[test_only]
    struct SomeStruct {}

    #[test]
    #[expected_failure]
    #[test, expected_failure = std::type_name::ENonModuleType]
    fun main() {}

    #[attr1, attr2]
    fun main2() {}

    #[test(a = @0x1, b = @0x2, c = @Std)]
    fun main3() {}

    #[test()]
    fun test_empty_parens() {}

    #[test_only]
    native fun native_main();

    #[show(book_orders_sdk, book_price_levels_sdk)]
    fun test() {}

    #[expected_failure(abort_code = liquidswap::liquidity_pool::ERR_ADMIN, location=0x1::liquidity_pool)]
    fun abort_test() {}

    #[allow(lint(self_transfer))]
    #[expected_failure(
        abort_code = liquidity_pool::ERR_ADMIN,
        location = aptos_framework::ed25519,
        location = aptos_framework::ed25519::myfunction,
    )]
    #[lint::allow_unsafe_randomness]
    fun abort_test_2() {}
}
