#[test]
module M {
    #[test_only]
    use 0x1::SomeOtherModule;

    #[test_only]
    struct SomeStruct {}

    #[test]
    #[expected_failure]
    fun main() {}

    #[attr1, attr2]
    fun main2() {}

    #[test(a = @0x1, b = @0x2)]
    fun main3() {}
}
