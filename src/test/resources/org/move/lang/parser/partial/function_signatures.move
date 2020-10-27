module M {
    fun main(a,) {}
    fun main(a: u8, b) {}
    fun main(a: u8, b:) {}
    fun main(): u8, {}
    fun main() acq
    fun main() acquires {}
    fun main() acquires U,,{}
    fun main() acquires U V{}

    fun main<() {}
    fun main<U() {}
    fun main<U,V,() {}
    fun main<U V>() {}
    fun main<U:, V: unknown>() {}
}