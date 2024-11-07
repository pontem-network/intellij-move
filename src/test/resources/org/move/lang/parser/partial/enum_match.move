module 0x1::enum_match {
    enum Color { Red, Blue }
    fun main() {
        match (s) {}
        match (s) { Red }
        match (s) { Red => }
        match (s) { Red => true, Blue => }

        match () {}
    }
}
