module M {
    fun
}

module M2 {
    fun main()
}

module M3 {
    use

    fun main() {}
}

module M4 {
    use 0x0::

    fun main() {}
}

module M5 {
    use 0x0::Transaction

    fun main() {}
}

module M6 {
    use 0x0::Transaction::create

    fun main() {}
}

module M7 {
    use 0x0::Transaction::create
    use 0x0::Transaction
    use 0x0::Transaction::modify;

    fun main() {}
}

module M81 {
    spec fun

    fun main() {}
}

module M82 {
    spec fun main

    fun main() {}
}

module M9 {
    struct MyStruct
    struct MyStruct2 {}
}

module M10 {
    struct MyStruct
    resource struct MyStruct2 {}
}
