address 0x1 {
    module M {
        spec fun main {
            assert 1 == 1;
            let a: address = 0x1;
        }
    }
}
address 0x0 {
    module M {
        fun main() {
            assert(1 == 1);
        }
    }
}