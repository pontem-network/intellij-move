module 0x1::cast_expr {
    fun main() {
        1 as u8;
        a as u8;
        1 + 1 as u8;
        (a as u8) + 1;
        call(a as u8);
    }
}
