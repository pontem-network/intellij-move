module M {
    spec
    spec module { assert 1 == 1; }

    fun main() { assert(1 == 1, 0); }
}