script {
    fun main() {
        let a = 1;
        let (a, b) = (1, 2);
        let R { a, b } = get_record();
        let R { a: _, b: _ } = get_record();

        let R { a: alias_a, b: alias_b } = get_record();
        let R { a: T { c, d }} = get_record();
        let R { a: T { c: alias_c, d: _ }} = get_record();

        let (R { a, b }, M { a: _, b: _ }) = get_record_tuple();

        let Generic<R> {} = g;
        let Generic<R> { g } = g;
        let Generic<R> { g: R { f: f3 } } = g;

        let a: (u8) = 1;
        let a: (((u8))) = 1;
        let b: ((u8, u8)) = (1, 1);
    }
}
