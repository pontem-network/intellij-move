script
module

module Std::

script {
    fun
}

script {
    fun main()
}

script {
    use
    fun main() {}
}

script {
    use 0x0::
}

script {
    use 0x0::

    fun main() {
        use 0x0::
    }

script {
    use 0x0::Transaction::

    fun main() {}

script {
    use 0x0::Transaction::create

    fun main() {}
}

script {
    use 0x0::Transaction::;

    fun main() {}
}

script {
    use 0x0::Transaction::create
    use 0x0::Transaction::modify;

    fun main() {}
}

module {
    use 0x0::Transaction::create
    use 0x0::Transaction::modify;

    fun main() {}
}

module {
    use 0x0::Transaction::create
    use 0x0::Transaction::modify;

    fun main() {}
}
