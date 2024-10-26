module 0x1::assign_bin_expr {
    fun main() {
        x += 1;
        x -= 1;
        x *= 1;
        x /= 1;
        x %= 1;

        x &= 1;
        x |= 1;
        x ^= 1;

        x <<= 1;
        x >>= 1;
    }
}
