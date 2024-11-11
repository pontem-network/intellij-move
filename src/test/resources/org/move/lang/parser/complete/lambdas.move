module 0x1::lambdas {
    fun main() {
        for_each(v, |f: &Function| {});
        for_each(v, |i: u8, g: u8| {});
    }
}
