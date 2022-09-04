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

    fun `test sort imports std first non test_only first case insensitive`() = doTest("""
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
    use std::signature;
    #[test_only]
    use Std::Errors;

    fun call(a: S1, b: S2, c: SS1) {
        Signer::address_of();
        Errors;
        signature;
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
    use std::signature;

    use AAA::M1::{S1, SS1};
    use BBB::M2::S2;

    #[test_only]
    use Std::Errors;

    fun call(a: S1, b: S2, c: SS1) {
        Signer::address_of();
        Errors;
        signature;
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

    fun `test self import into module import if no items`() = doTest("""
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
    use 0x1::Coin;

    fun main() {
        Coin::call();
    }
}        
    """)

    fun `test module spec`() = doTest("""
module 0x1::string {}        
spec 0x1::main {
    use 0x1::string;
}        
    """, """
module 0x1::string {}        
spec 0x1::main {}        
    """)

    fun `test test_only merged into main`() = doTest("""
module 0x1::string { public fun utf8() {} }        
module 0x1::main {
    use 0x1::string::utf8;
    #[test_only]
    use 0x1::string::utf8;
    fun main() {
        utf8();
    }
}        
    """, """
module 0x1::string { public fun utf8() {} }        
module 0x1::main {
    use 0x1::string::utf8;
    fun main() {
        utf8();
    }
}        
    """)

//    fun `test module spec with parent import`() = doTest("""
//module 0x1::string { public fun utf8(v: vector<u8>) {} }
//module 0x1::main {
//    use 0x1::string;
//
//    fun main() {
//        let _a = string::utf8(b"hello");
//    }
//}
//spec 0x1::main {
//    use 0x1::string;
//
//    spec main {
//        let _a = string::utf8(b"hello");
//    }
//}
//    """, """
//module 0x1::string { public fun utf8(v: vector<u8>) {} }
//module 0x1::main {
//    use 0x1::string;
//
//    fun main() {
//        let _a = string::utf8(b"hello");
//    }
//}
//spec 0x1::main {
//    spec main {
//        let _a = string::utf8(b"hello");
//    }
//}
//    """)

    private fun doTest(@Language("Move") before: String, @Language("Move") after: String) =
        checkEditorAction(before, after, "OptimizeImports")
}
