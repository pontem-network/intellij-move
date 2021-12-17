#[test]
module 0x1::M {
    #[test_only]
    use 0x1::SomeOtherModule;

    #[test_only]
    struct SomeStruct {}

    #[test_only]
    native struct S;

    #[test]
    #[expected_failure]
    fun main() {}

    #[attr1, attr2]
    fun main2() {}

    #[test(a = @0x1, b = @0x2, c = @Std)]
    fun main3() {}

    #[test_only]
    native fun native_main();
}
