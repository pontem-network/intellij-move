package org.move.ide.refactoring.optimizeImports

class MergeImportsTest : OptimizeImportsTestBase() {

    fun `test self import unchanged if no items`() = doTest("""
module 0x1::Coin { public fun call() {} }        
module 0x1::Main {
    use 0x1::Coin::Self;
    fun main() {
        Coin::call();
    }
}        
    """, """
module 0x1::Coin { public fun call() {} }        
module 0x1::Main {
    use 0x1::Coin::Self;

    fun main() {
        Coin::call();
    }
}        
    """)

//    fun `test merge item into existing group`() = doTest(
//        """
//module 0x1::M1 { struct S1 {} struct S2 {} struct S3 {} }
//module 0x1::Main {
//    use 0x1::M1::S1;
//    use 0x1::M1::{S2, S3};
//
//    fun call(s1: S1, s2: S2, s3: S3) {}
//}
//    """, """
//module 0x1::M1 { struct S1 {} struct S2 {} struct S3 {} }
//module 0x1::Main {
//    use 0x1::M1::{S1, S2, S3};
//
//    fun call(s1: S1, s2: S2, s3: S3) {}
//}
//    """
//    )
//
//    fun `test simple module import merges with item group`() = doTest(
//        """
//    module 0x1::Coin {
//        struct Coin {}
//        public fun get_coin(): Coin {}
//    }
//    module 0x1::Main {
//        use 0x1::Coin;
//        use 0x1::Coin::Coin;
//
//        fun call(): Coin {
//            Coin::get_coin()
//        }
//    }
//    """, """
//    module 0x1::Coin {
//        struct Coin {}
//        public fun get_coin(): Coin {}
//    }
//    module 0x1::Main {
//        use 0x1::Coin::{Self, Coin};
//
//        fun call(): Coin {
//            Coin::get_coin()
//        }
//    }
//    """
//    )
//
//    fun `test test_only has its own separate group`() = doTest("""
//    module 0x1::Coin {
//        struct Coin {}
//        struct Coin2 {}
//        public fun get_coin(): Coin {}
//        #[test_only]
//        public fun get_coin_2(): Coin {}
//    }
//    module 0x1::Main {
//        use 0x1::Coin;
//        use 0x1::Coin::Coin;
//        #[test_only]
//        use 0x1::Coin::Coin2;
//        #[test_only]
//        use 0x1::Coin::get_coin_2;
//
//        fun call(): Coin {
//            Coin::get_coin()
//        }
//
//        #[test]
//        fun test(c: Coin2) {
//            get_coin_2();
//        }
//    }
//    """, """
//    module 0x1::Coin {
//        struct Coin {}
//        struct Coin2 {}
//        public fun get_coin(): Coin {}
//        #[test_only]
//        public fun get_coin_2(): Coin {}
//    }
//    module 0x1::Main {
//        use 0x1::Coin::{Self, Coin};
//
//        #[test_only]
//        use 0x1::Coin::{Coin2, get_coin_2};
//
//        fun call(): Coin {
//            Coin::get_coin()
//        }
//
//        #[test]
//        fun test(c: Coin2) {
//            get_coin_2();
//        }
//    }
//    """)

//    fun `test merge items into group`() = doTest("""
//module 0x1::M1 { struct S1 {} struct S2 {} }
//module 0x1::Main {
//    use 0x1::M1::S1;
//    use 0x1::M1::S2;
//
//    fun call(s1: S1, s2: S2) {}
//}
//    """, """
//module 0x1::M1 { struct S1 {} struct S2 {} }
//module 0x1::Main {
//    use 0x1::M1::{S1, S2};
//
//    fun call(s1: S1, s2: S2) {}
//}
//    """)

}
