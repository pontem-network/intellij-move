module 0x1::function_values_lambda_type {
    struct Predicate<T>(|&T|bool) has copy;

    fun main(
        a: |bool| SettleTradeResult has drop + copy,
        a: |bool, address| Option<u64> has drop + copy,
    ) {
        let f: |u64|bool has copy = |x| x > 0;

        let f: Predicate<u64> = |x| *x > 0;
        assert!(f(&22));
    }
}
