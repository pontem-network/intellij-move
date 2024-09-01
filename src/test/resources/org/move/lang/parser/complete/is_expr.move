module 0x1::is_expr {
    fun main() {
        s is One;
        s is One|Two;
        s is One | Two;
        if (s is One::Two) {}
    }
}
