module M {
    fun main(a,) {}
    fun main(a: u8, b) {}
    fun main(a: u8, b:) {}
    fun main(): u8, {}
    fun main() acquires {}
    fun main() acquires U,,{}
    fun main() acquires U V{}
}