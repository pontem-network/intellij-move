package org.move.ide.refactoring

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class MvImportOptimizerTest : MvTestBase() {
    fun `test no change`() = doTest("""
script {
    use 0x1::M::MyStruct;
    use 0x1::M::call;
    
    fun main() {
        let a: MyStruct = call();
    }
}
    """, """
script {
    use 0x1::M::MyStruct;
    use 0x1::M::call;

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

    fun `test sort imports std first`() = doTest("""
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
    use Std::Errors;
    use Std::Signer;

    use AAA::M1::S1;
    use AAA::M1::SS1;
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

    private fun doTest(@Language("Move") code: String, @Language("Move") expected: String) =
        checkEditorAction(code, expected, "OptimizeImports")
}
