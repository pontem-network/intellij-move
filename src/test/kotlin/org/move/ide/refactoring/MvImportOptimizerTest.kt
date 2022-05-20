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

//    fun `test remove duplicate module import`() = doTest("""
//module 0x1::M { fun m() {} }
//module 0x1::M2 {
//    use 0x1::M;
//    use 0x1::M;
//    fun main() {
//        M::m();
//    }
//}
//    """, """
//module 0x1::M { fun m() {} }
//module 0x1::M2 {
//    use 0x1::M;
//    fun main() {
//        M::m();
//    }
//}
//    """)

    private fun doTest(@Language("Move") code: String, @Language("Move") excepted: String) =
        checkEditorAction(code, excepted, "OptimizeImports")
}
