package org.move.ide.refactoring.optimizeImports

class OptimizeImportsTest : OptimizeImportsTestBase() {
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

    fun `test remove unused import from group in the middle`() = doTest("""
        module 0x1::M {
            struct MyStruct {}
            public fun call() {}
            public fun aaa() {}
        }        
        script {
            use 0x1::M::{aaa, MyStruct, call};
            
            fun main() {
                let a = call();
                let a = aaa();
            }
        }
    """, """
        module 0x1::M {
            struct MyStruct {}
            public fun call() {}
            public fun aaa() {}
        }        
        script {
            use 0x1::M::{aaa, call};
            
            fun main() {
                let a = call();
                let a = aaa();
            }
        }
    """)

    fun `test remove unused import from group in the beginning`() = doTest(
        """
        module 0x1::M {
            struct Bbb {}
            public fun call() {}
            public fun aaa() {}
        }        
        script {
            use 0x1::M::{aaa, Bbb, call};
            
            fun main() {
                let a: Bbb = call();
            }
        }
    """, """
        module 0x1::M {
            struct Bbb {}
            public fun call() {}
            public fun aaa() {}
        }        
        script {
            use 0x1::M::{Bbb, call};
            
            fun main() {
                let a: Bbb = call();
            }
        }
    """)

    fun `test remove unused import from group in the end`() = doTest(
        """
        module 0x1::M {
            struct Bbb {}
            public fun call() {}
            public fun aaa() {}
        }        
        script {
            use 0x1::M::{aaa, Bbb, call};
            
            fun main() {
                let a: Bbb = aaa();
            }
        }
    """, """
        module 0x1::M {
            struct Bbb {}
            public fun call() {}
            public fun aaa() {}
        }        
        script {
            use 0x1::M::{aaa, Bbb};
            
            fun main() {
                let a: Bbb = aaa();
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
            use endless_stdlib::table;
            use endless_stdlib::iter_table;
            use endless_framework::coin;
            #[test_only]
            use Std::Errors;
            use Std::Signer;
            use std::signature;
        
            fun call(a: S1, b: S2, c: SS1) {
                Signer::address_of();
                signature::;
                table::;
                iter_table::;
                coin::;
            }
        
            #[test]
            fun test() {
                Errors::;
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
            use endless_stdlib::iter_table;
            use endless_stdlib::table;
            use endless_framework::coin;
        
            use AAA::M1::S1;
            use AAA::M1::SS1;
            use BBB::M2::S2;
        
            #[test_only]
            use Std::Errors;
        
            fun call(a: S1, b: S2, c: SS1) {
                Signer::address_of();
                signature::;
                table::;
                iter_table::;
                coin::;
            }
        
            #[test]
            fun test() {
                Errors::;
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

    fun `test module spec`() = doTest("""
module 0x1::string {}        
spec 0x1::main {
    use 0x1::string;
}        
    """, """
module 0x1::string {}        
spec 0x1::main {}        
    """)

    fun `test duplicate struct import`() = doTest("""
module 0x1::pool { 
    struct X1 {}    
    public fun create_pool<BinStep>() {}        
}        
module 0x1::main {
    use 0x1::pool::{Self, X1, X1};

    fun main() {
        pool::create_pool<X1>();
    }
}        
    """, """
module 0x1::pool { 
    struct X1 {}    
    public fun create_pool<BinStep>() {}        
}        
module 0x1::main {
    use 0x1::pool::{Self, X1};

    fun main() {
        pool::create_pool<X1>();
    }
}        
    """)

    fun `test unused import with Self as`() = doTest("""
module 0x1::pool { 
    struct X1 {}    
    public fun create_pool() {}        
}        
module 0x1::main {
    use 0x1::pool::{Self as mypool, X1};

    fun main() {
        mypool::create_pool();
    }
}
    """, """
module 0x1::pool { 
    struct X1 {}    
    public fun create_pool() {}        
}        
module 0x1::main {
    use 0x1::pool::Self as mypool;

    fun main() {
        mypool::create_pool();
    }
}
    """)

    fun `test duplicate self import`() = doTest("""
        module 0x1::pool { 
            struct X1 {}    
            public fun create_pool<BinStep>() {}        
        }        
        module 0x1::main {
            use 0x1::pool::{Self, Self, X1};
        
            fun main() {
                pool::create_pool<X1>();
            }
        }        
    """, """
        module 0x1::pool { 
            struct X1 {}    
            public fun create_pool<BinStep>() {}        
        }        
        module 0x1::main {
            use 0x1::pool::{Self, X1};
        
            fun main() {
                pool::create_pool<X1>();
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

    // todo
//    fun `test vector import should be test_only if used only in tests`() = doTest("""
//module 0x1::vector {
//    public fun call() {}
//}
//module 0x1::main {
//    use 0x1::vector;
//
//    #[test]
//    fun test_main() {
//        vector::call();
//    }
//}
//    """, """
//module 0x1::vector {
//    public fun call() {}
//}
//module 0x1::main {
//    #[test_only]
//    use 0x1::vector;
//
//    #[test]
//    fun test_main() {
//        vector::call();
//    }
//}
//    """)
}
