package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class MvUnusedImportInspectionTest: InspectionTestBase(MvUnusedImportInspection::class) {
    fun `test no error`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    use 0x1::M::MyItem;
    use 0x1::M::MyItem2;
    use 0x1::M::call;
    fun main(arg: MyItem2) {
        let a: MyItem = call();
    }
}        
    """)

    fun `test error unused item import`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    <warning descr="Unused use item">use 0x1::M::MyItem;</warning>
    fun main() {}
}
    """)

    fun `test error unused module import`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    <warning descr="Unused use item">use 0x1::M;</warning>
    fun main() {}
}
    """)

    fun `test unused item in use group`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    use 0x1::M::{MyItem, <warning descr="Unused use item">MyItem2</warning>};
    fun main(a: MyItem) {}
}
    """)

    fun `test no error if module imported and used as fq`() = checkWarnings("""
module 0x1::M {
    public fun call() {}
}
module 0x1::M2 {
    use 0x1::M;
    fun main() {
        M::call();
    }
}
    """)

    fun `test no unused import on Self`() = checkWarnings("""
module 0x1::M {
    struct S {}
    public fun call() {}
}        
module 0x1::Main {
    use 0x1::M::{Self, S};
    
    fun main(a: S) {
        M::call();
    }
}
    """)

    fun `test unused imports if unresolved module`() = checkWarnings("""
module 0x1::Main {
    <warning descr="Unused use item">use 0x1::M1;</warning>
}
    """)

    fun `test no unused import if unresolved module but used`() = checkWarnings("""
module 0x1::Main {
    use 0x1::M;
    fun call() {
        M::call();
    }
}        
    """)

    fun `test unused imports if unresolved item`() = checkWarnings("""
module 0x1::Main {
    <warning descr="Unused use item">use 0x1::M1::call;</warning>
}
    """)

    fun `test no unused import if unresolved item but used`() = checkWarnings("""
module 0x1::Main {
    use 0x1::M::call;
    fun main() {
        call();
    }
}        
    """)

    fun `test duplicate import`() = checkWarnings("""
module 0x1::M {
    public fun call() {}
}
module 0x1::M2 {
    use 0x1::M::call;
    <warning descr="Unused use item">use 0x1::M::call;</warning>

    fun main() {
        call();
    }
}
    """)

    fun `test duplicate import with item group`() = checkWarnings("""
module 0x1::M {
    struct S {}
    public fun call() {}
}
module 0x1::M2 {
    use 0x1::M::{S, call};
    <warning descr="Unused use item">use 0x1::M::call;</warning>

    fun main(s: S) {
        call();
    }
}
    """)

    fun `test unused Self import`() = checkWarnings("""
    module 0x1::Coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::Coin;
        <warning descr="Unused use item">use 0x1::Coin::Self;</warning>
        
        fun call(): Coin {
            Coin::get_coin()
        }
    }
    """)

    fun `test unused Self in group`() = checkWarnings("""
    module 0x1::Coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::Coin;
        use 0x1::Coin::{<warning descr="Unused use item">Self</warning>, Coin};
        
        fun call(): Coin {
            Coin::get_coin()
        }
    }
    """)

    fun `test skip analyzing for incomplete self item alias`() = checkWarnings("""
    module 0x1::coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::coin::{Self as<error descr="IDENTIFIER expected, got ','">,</error> Self, Coin};
        
        fun call(): Coin {
            coin::get_coin();
        }
    }
    """)

    fun `test skip analyzing for incomplete item alias`() = checkWarnings("""
    module 0x1::coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::coin::{Coin as<error descr="IDENTIFIER expected, got ','">,</error> Coin};
        
        fun call(): Coin {
        }
    }
    """)

    fun `test no unused Self in group with alias and no alias`() = checkWarnings("""
    module 0x1::coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::coin::{Self as my_coin, Self, Coin};
        
        fun call(): Coin {
            coin::get_coin();
            my_coin::get_coin();
        }
    }
    """)

    fun `test unused alias if another exists`() = checkWarnings("""
    module 0x1::Coin {
        struct Coin {}
        public fun get_coin(): Coin {}
    }    
    module 0x1::Main {
        use 0x1::Coin::{Coin as MyCoin, <warning descr="Unused use item">Coin as MyCoin</warning>};
        
        fun call(): MyCoin {}
    }
    """)

    fun `test empty item group`() = checkWarnings("""
    module 0x1::Coin {
        struct C {}
    }    
    module 0x1::Main {
        <warning descr="Unused use item">use 0x1::Coin::{};</warning>
    }
    """)

    fun `test all items in group unused`() = checkWarnings("""
    module 0x1::Coin {
        struct C {}
        struct D {}
    }    
    module 0x1::Main {
        <warning descr="Unused use item">use 0x1::Coin::{C, D};</warning>
    }
    """)

    fun `test unused import in module spec`() = checkWarnings("""
    spec 0x1::Main {
        <warning descr="Unused use item">use 0x1::Coin::{};</warning>
    }
    """)

    fun `test unused signer import`() = checkWarnings("""
    module 0x1::main {
        <warning descr="Unused use item">use std::signer;</warning>
        fun call(a: signer) {}
    }    
    """)

    fun `test unused signer import with self`() = checkWarnings("""
    module 0x1::main {
        <warning descr="Unused use item">use std::signer::Self;</warning>
        fun call(a: signer) {}
    }    
    """)

    fun `test unused vector import`() = checkWarnings("""
    module 0x1::main {
        <warning descr="Unused use item">use std::vector;</warning>
        fun call(a: vector<u8>) {}
    }    
    """)

    fun `test unused module import type with the same name is used`() = checkWarnings("""
module 0x1::main {
    <warning descr="Unused use item">use std::coin;</warning>
    use std::coin::coin;
    
    fun call(coin: coin) {}
}        
    """)

    fun `test no unused import for type`() = checkWarnings("""
module 0x1::main {
    use std::coin::coin;
    fun call(coin: coin) {
    }
}        
    """)


    fun `test no unused import for type with the same name as module`() = checkWarnings("""
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

    // TODO: test
//    fun `test unused test_only import`() = checkWarnings("""
//    module 0x1::string {
//        public fun call() {}
//    }
//    module 0x1::main {
//        use 0x1::string::call;
//        <warning descr="Unused use item">#[test_only]
//        use 0x1::string::call;</warning>
//
//        fun main() {
//            call();
//        }
//        #[test]
//        fun test_main() {
//            call();
//        }
//    }
//    """)

    fun `test unused main import in presence of test_only usage`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }        
    module 0x1::main {
        <warning descr="Unused use item">use 0x1::string::call;</warning>
        #[test_only]
        use 0x1::string::call;
        
        #[test_only]
        fun main() {
            call();
        }
    }
    """)

    fun `test unused main import in presence of unresolved test_only usage`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }        
    module 0x1::main {
        <warning descr="Unused use item">use 0x1::string::call;</warning>

        #[test_only]
        fun main() {
            call();
        }
    }
    """)

    fun `test no error with used alias`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }        
    module 0x1::main {
        use 0x1::string::call as mycall;

        fun main() {
            mycall();
        }
    }
    """)

    fun `test no error with used module alias`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }        
    module 0x1::main {
        use 0x1::string as mystring;

        fun main() {
            mystring::call();
        }
    }
    """)

    fun `test error with unused alias`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }        
    module 0x1::main {
        <warning descr="Unused use item">use 0x1::string::call as mycall;</warning>

        fun main() {
        }
    }
    """)

    fun `test error with unused module alias`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }        
    module 0x1::main {
        <warning descr="Unused use item">use 0x1::string as mystring;</warning>

        fun main() {
        }
    }
    """)

    fun `test error with self module alias used in type position`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }    
    module 0x1::main {
        <warning descr="Unused use item">use 0x1::string::Self as mystring;</warning>

        fun main(s: mystring) {
        }
    }
    """)

    fun `test no error with self module alias`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }    
    module 0x1::main {
        use 0x1::string::Self as mystring;

        fun main() {
            mystring::call();
        }
    }
    """)

    fun `test no error with self module alias in group`() = checkWarnings("""
    module 0x1::string {
        public fun call() {}
    }    
    module 0x1::main {
        use 0x1::string::{Self as mystring};

        fun main() {
            mystring::call();
        }
    }
    """)

    fun `test no unused import for function return type`() = checkWarnings("""
module 0x1::string {
    struct String {}
}    
module 0x1::main {
    use 0x1::string;
    public fun type_name<T>(): string::String {}
}        
    """)

    fun `test no unused import for native function return type`() = checkWarnings("""
module 0x1::string {
    struct String {}
}    
module 0x1::main {
    use 0x1::string;
    public native fun type_name<T>(): string::String;
}        
    """)

    // TODO: test
//    fun `test unused top import with local present`() = checkWarnings("""
//        module 0x1::string {
//            public fun call() {}
//        }
//        module 0x1::main {
//            <warning descr="Unused use item">use 0x1::string;</warning>
//            fun main() {
//                use 0x1::string;
//                string::call();
//            }
//        }
//    """)

    fun `test unused local import`() = checkWarnings("""
        module 0x1::string {
            public fun call() {}
        }
        module 0x1::main {
            fun main() {
                <warning descr="Unused use item">use 0x1::string;</warning>
            }
        }
    """)

    fun `test no unused import if used in two local places`() = checkWarnings("""
module 0x1::string {
    public fun call() {}
}    
module 0x1::main {
    use 0x1::string;
    fun a() {
        string::call();
    }
    fun b() {
        use 0x1::string;
        string::call();
    }
}        
    """)

    fun `test unused import if imported locally test_only`() = checkWarnings("""
module 0x1::string {
    public fun call() {}
}    
module 0x1::main {
    <warning descr="Unused use item">use 0x1::string;</warning>
    #[test_only]
    fun main() {
        use 0x1::string;
        string::call();
    }
}        
    """)

    fun `test no unused import used in both main and test scopes expr`() = checkWarnings("""
module 0x1::string {
    struct String {}
    public fun call() {}
}    
module 0x1::main {
    use 0x1::string;
    fun d() {
        string::call();
    }
    #[test_only]
    fun main() {
        string::call();
    }
}        
    """)

    fun `test no unused import used in both main and test scopes type`() = checkWarnings("""
module 0x1::string {
    struct String {}
    public fun call() {}
}    
module 0x1::main {
    use 0x1::string;
    struct S { val: string::String }
    #[test_only]
    fun main() {
        string::call();
    }
}        
    """)

    fun `test no unused import on dot object with same name as module`() = checkWarnings("""
module 0x1::m {
    #[view]
    public fun call() {}
}        
module 0x1::main {
    use 0x1::m;
    public entry fun main() {
        if (m::call()) m::call();
    }
}
spec 0x1::main {
    spec main {
        let m = 1;
        m.addr = 1;
    }    
}
    """)
}
