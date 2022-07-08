script {
    use 0x1::Transaction;
    use 0x1::Transaction::Instance;
    use 0x1::Transaction::{};
    use 0x1::Transaction::{Self, EventHandle};
    use 0x1::Transaction::foo as bar;
    use 0x1::Transaction::{foo, foo as bar};

    fun main() {
        use 0x1::Transaction;

        let x = {
            use 0x1::Transaction;
            0
        };

        {{{{
            use 0x2::Mango;
        }}}};

        while (true) {
            use 0x1::Transaction;
        };
    }
}
