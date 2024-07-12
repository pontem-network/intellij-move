module 0x1::m {
    use 0x1::Transaction;
    use 0x1::Transaction::Instance;
    use 0x1::Transaction::{};
    use 0x1::Transaction::{Self, EventHandle};
    use 0x1::Transaction::foo as bar;
    use 0x1::Transaction::{foo, foo as bar, foo::Handle};

    use liquidswap::Transaction::call;
    use liquidswap::Transaction::call::Handle;
    use liquidswap::Transaction::call::{Handle1, Handle2};

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
