package org.move.ide.refactoring

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class ImportOptimizerTest : MvTestBase() {
    fun `test no change`() = doTest("""
script {
    use 0x1::M::{MyStruct, call};
    
    fun main() {
        let a: MyStruct = call();
    }
}
    """, """
script {
    use 0x1::M::{MyStruct, call};

    fun main() {
        let a: MyStruct = call();
    }
}
    """)

    fun `test remove unused struct import`() = doTest("""
module 0x1::M {
    struct MyStruct {}
    public fun call() {}
}        
script {
    use 0x1::M::MyStruct;
    use 0x1::M::call;
    
    fun main() {
        let a = call();
    }
}
    """, """
module 0x1::M {
    struct MyStruct {}
    public fun call() {}
}        
script {
    use 0x1::M::call;

    fun main() {
        let a = call();
    }
}
    """)

    fun `test remove unused import from group`() = doTest("""
module 0x1::M {
    struct MyStruct {}
    public fun call() {}
}        
script {
    use 0x1::M::{MyStruct, call};
    
    fun main() {
        let a = call();
    }
}
    """, """
module 0x1::M {
    struct MyStruct {}
    public fun call() {}
}        
script {
    use 0x1::M::call;

    fun main() {
        let a = call();
    }
}
    """)

    fun `test remove curly braces`() = doTest("""
module 0x1::M {
    struct MyStruct {}
    public fun call() {}
}        
script {
    use 0x1::M::{call};
    
    fun main() {
        let a = call();
    }
}
    """, """
module 0x1::M {
    struct MyStruct {}
    public fun call() {}
}        
script {
    use 0x1::M::call;

    fun main() {
        let a = call();
    }
}
    """)

    fun `test remove unused module import`() = doTest("""
module 0x1::M {}
module 0x1::M2 {
    use 0x1::M;
}        
    """, """
module 0x1::M {}
module 0x1::M2 {}        
    """)

    fun `test remove unused import group with two items`() = doTest("""
module 0x1::M {
    struct BTC {}
    struct USDT {}
}        
module 0x1::Main {
    use 0x1::M::{BTC, USDT};
}
    """, """
module 0x1::M {
    struct BTC {}
    struct USDT {}
}        
module 0x1::Main {}
    """)

    fun `test sort imports std first non test_only first`() = doTest("""
module AAA::M1 {
    struct S1 {} 
    struct SS1 {} 
}        
module BBB::M2 {
    struct S2 {}
}
module 0x1::Main {
    use BBB::M2::S2;
    use AAA::M1::S1;
    use AAA::M1::SS1;
    use Std::Signer;
    #[test_only]
    use Std::Errors;

    fun call(a: S1, b: S2, c: SS1) {
        Signer::address_of();
        Errors;
    }
}
    """, """
module AAA::M1 {
    struct S1 {} 
    struct SS1 {} 
}        
module BBB::M2 {
    struct S2 {}
}
module 0x1::Main {
    use Std::Signer;
    #[test_only]
    use Std::Errors;

    use AAA::M1::{S1, SS1};
    use BBB::M2::S2;

    fun call(a: S1, b: S2, c: SS1) {
        Signer::address_of();
        Errors;
    }
}
    """)

    fun `test remove all imports if not needed`() = doTest("""
module Std::Errors {}        
module Std::Signer {}        
module AAA::M1 {
    struct S1 {} 
    struct SS1 {} 
}        
module BBB::M2 {
    struct S2 {}
}
module 0x1::Main {
    use Std::Errors;
    use Std::Signer;

    use AAA::M1::S1;
    use AAA::M1::SS1;
    use BBB::M2::S2;

    #[test]
    fun call() {}
}
    """, """
module Std::Errors {}        
module Std::Signer {}        
module AAA::M1 {
    struct S1 {} 
    struct SS1 {} 
}        
module BBB::M2 {
    struct S2 {}
}
module 0x1::Main {
    #[test]
    fun call() {}
}
    """)

    fun `test removes empty group`() = doTest("""
module 0x1::M1 {}         
module 0x1::Main {
    use 0x1::M1::{};
}        
    """, """
module 0x1::M1 {}         
module 0x1::Main {}        
    """)

    fun `test merge items into group`() = doTest("""
module 0x1::M1 { struct S1 {} struct S2 {} }         
module 0x1::Main {
    use 0x1::M1::S1;
    use 0x1::M1::S2;
    
    fun call(s1: S1, s2: S2) {}
}        
    """, """
module 0x1::M1 { struct S1 {} struct S2 {} }         
module 0x1::Main {
    use 0x1::M1::{S1, S2};

    fun call(s1: S1, s2: S2) {}
}        
    """)

    fun `test merge item into existing group`() = doTest("""
module 0x1::M1 { struct S1 {} struct S2 {} struct S3 {} }         
module 0x1::Main {
    use 0x1::M1::S1;
    use 0x1::M1::{S2, S3};
    
    fun call(s1: S1, s2: S2, s3: S3) {}
}        
    """, """
module 0x1::M1 { struct S1 {} struct S2 {} struct S3 {} }         
module 0x1::Main {
    use 0x1::M1::{S1, S2, S3};

    fun call(s1: S1, s2: S2, s3: S3) {}
}        
    """)

    fun `test simple module import merges with item group`() = doTest("""
    module 0x1::Coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::Coin;
        use 0x1::Coin::Coin;
        
        fun call(): Coin {
            Coin::get_coin()
        }
    }
    """, """
    module 0x1::Coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::Coin::{Self, Coin};
    
        fun call(): Coin {
            Coin::get_coin()
        }
    }
    """)

    fun `test test_only has its own separate group`() = doTest("""
    module 0x1::Coin {
        struct Coin {}
        struct Coin2 {}
        public fun get_coin(): Coin {}
        #[test_only]
        public fun get_coin_2(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::Coin;
        use 0x1::Coin::Coin;
        #[test_only]
        use 0x1::Coin::Coin2;
        #[test_only]
        use 0x1::Coin::get_coin_2;
        
        fun call(c: Coin2): Coin {
            get_coin_2();
            Coin::get_coin()
        }
    }
    """, """
    module 0x1::Coin {
        struct Coin {}
        struct Coin2 {}
        public fun get_coin(): Coin {}
        #[test_only]
        public fun get_coin_2(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::Coin::{Self, Coin};
        #[test_only]
        use 0x1::Coin::{Coin2, get_coin_2};
    
        fun call(c: Coin2): Coin {
            get_coin_2();
            Coin::get_coin()
        }
    }
    """)

    private fun doTest(@Language("Move") before: String, @Language("Move") after: String) =
        checkEditorAction(before, after, "OptimizeImports")
}
