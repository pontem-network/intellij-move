package org.move.ide.refactoring

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class RenameTest : MvTestBase() {
    fun `test function argument`() = doTest(
        "spam", """
        script {
            fun main(/*caret*/account: &signer) {
                account;
            }
        }
    """, """
        script {
            fun main(spam: &signer) {
                spam;
            }
        }
    """
    )

    fun `test local variable`() = doTest(
        "spam", """
        script {
            fun main() {
                let /*caret*/a = 1;
                a;
            }
        }
    """, """
        script {
            fun main() {
                let spam = 1;
                spam;
            }
        }
    """
    )

    fun `test local variable shadowed`() = doTest(
        "spam", """
        script {
            fun main() {
                let /*caret*/a = 1;
                let a = a + 1;
                a;
            }
        }
    """, """
        script {
            fun main() {
                let spam = 1;
                let a = spam + 1;
                a;
            }
        }
    """
    )

    fun `test function name`() = doTest(
        "renamed_call", """
        module M {
            fun /*caret*/call() {
                1
            }

            fun main() {
                call();
            }
        }
    """, """
        module M {
            fun renamed_call() {
                1
            }

            fun main() {
                renamed_call();
            }
        }
    """
    )

    fun `test destructuring field reassignment`() = doTest(
        "myval2", """
        module M {
            struct MyStruct { val: u8 }
            fun main() {
                let MyStruct { val: myval } = get_struct();
                /*caret*/myval;
            }
        }
    """, """
        module M {
            struct MyStruct { val: u8 }
            fun main() {
                let MyStruct { val: myval2 } = get_struct();
                myval2;
            }
        }
    """
    )

    fun `test rename val with shorthand struct literal`() = doTest(
        "myval", """
        module 0x1::M {
            struct MyStruct { val: u8 }
            fun main() {
                let val = 1;
                MyStruct { val };
                /*caret*/val;
            }
        }
    """, """
        module 0x1::M {
            struct MyStruct { val: u8 }
            fun main() {
                let myval = 1;
                MyStruct { val: myval };
                myval;
            }
        }
    """
    )

    fun `test rename field from shorthand struct literal`() = doTest(
        "myval", """
        module 0x1::M {
            struct MyStruct { /*caret*/val: u8 }
            fun main() {
                let val = 1;
                MyStruct { val };
                val;
            }
        }
    """, """
        module 0x1::M {
            struct MyStruct { myval: u8 }
            fun main() {
                let val = 1;
                MyStruct { myval: val };
                val;
            }
        }
    """
    )

    fun `test rename val from shorthand struct pattern`() = doTest(
        "myval", """
        module 0x1::M {
            struct MyStruct { val: u8 }
            fun get_s(): MyStruct { MyStruct { val: 1 } }
            fun main() {
                let MyStruct { val } = get_s();
                /*caret*/val;
            }
        }
    """, """
        module 0x1::M {
            struct MyStruct { val: u8 }
            fun get_s(): MyStruct { MyStruct { val: 1 } }
            fun main() {
                let MyStruct { val: myval } = get_s();
                myval;
            }
        }
    """
    )

    fun `test rename field from shorthand struct pattern`() = doTest(
        "myval", """
        module 0x1::M {
            struct MyStruct { /*caret*/val: u8 }
            fun get_s(): MyStruct { MyStruct { val: 1 } }
            fun main() {
                let MyStruct { val } = get_s();
                val;
            }
        }
    """, """
        module 0x1::M {
            struct MyStruct { myval: u8 }
            fun get_s(): MyStruct { MyStruct { myval: 1 } }
            fun main() {
                let MyStruct { myval: val } = get_s();
                val;
            }
        }
    """
    )

    fun `test struct`() = doTest(
        "RenamedStruct", """
        module M {
            struct /*caret*/MyStruct { val: u8 }
            
            fun main(s: MyStruct): MyStruct {
                let MyStruct { val: myval } = get_struct();
                let a = MyStruct { val: 1 };
                move_from<MyStruct>();
            }
        }
    """, """
        module M {
            struct RenamedStruct { val: u8 }
            
            fun main(s: RenamedStruct): RenamedStruct {
                let RenamedStruct { val: myval } = get_struct();
                let a = RenamedStruct { val: 1 };
                move_from<RenamedStruct>();
            }
        }
    """
    )

    fun `test struct from use position`() = doTest(
        "RenamedStruct", """
        module M {
            struct MyStruct has key { val: u8 }
            
            fun main(s: MyStruct): /*caret*/MyStruct {
                let MyStruct { val: myval } = get_struct();
                let a = MyStruct { val: 1 };
                move_from<MyStruct>();
            }
        }
    """, """
        module M {
            struct RenamedStruct has key { val: u8 }
            
            fun main(s: RenamedStruct): RenamedStruct {
                let RenamedStruct { val: myval } = get_struct();
                let a = RenamedStruct { val: 1 };
                move_from<RenamedStruct>();
            }
        }
    """
    )

//    fun `test schema`() = doTest("RenamedSchema", """
//        module M {
//            spec schema /*caret*/MySchema {}
//
//            spec module {
//                apply MySchema to *;
//            }
//        }
//    """, """
//        module M {
//            spec schema RenamedSchema {}
//
//            spec module {
//                apply RenamedSchema to *;
//            }
//        }
//    """)

    fun `test type param`() = doTest(
        "U", """
        module M {
            struct MyStruct<T> {
                val: /*caret*/T
            }
        }
    """, """
        module M {
            struct MyStruct<U> {
                val: U
            }
        }
    """
    )

    fun `test const`() = doTest(
        "RENAMED_CONST", """
        module 0x1::M {
            const /*caret*/MY_CONST: u8 = 1;
            
            fun main(): u8 {
                MY_CONST
            }
        }
    """, """
        module 0x1::M {
            const RENAMED_CONST: u8 = 1;
            
            fun main(): u8 {
                RENAMED_CONST
            }
        }
    """
    )

    fun `test native struct`() = doTest(
        "RenamedNative", """
        module M {
            native struct /*caret*/Native<T>;
            
            native fun main(n: Native<u8>): u8;
        }
    """, """
        module M {
            native struct RenamedNative<T>;
            
            native fun main(n: RenamedNative<u8>): u8;
        }
    """
    )

    fun `test import alias`() = doTest(
        "RenamedStruct", """
        address 0x1 {
            module Transaction {
                struct Struct {}
            }
            
            module M {
                use 0x1::Transaction::Struct as MyStruct;
    
                fun main(): /*caret*/MyStruct {}
            }
        }
    """, """
        address 0x1 {
            module Transaction {
                struct Struct {}
            }
            
            module M {
                use 0x1::Transaction::Struct as RenamedStruct;
    
                fun main(): RenamedStruct {}
            }
        }
    """
    )

    fun `test rename module via import`() = doTest(
        "RenamedTransaction", """
        address 0x1 {
            module Transaction {}
            
            module M {
                use 0x1::Transaction;
    
                fun main() {
                    let a = /*caret*/Transaction::create();
                }
            }
        }
    """, """
        address 0x1 {
            module RenamedTransaction {}
            
            module M {
                use 0x1::RenamedTransaction;
    
                fun main() {
                    let a = RenamedTransaction::create();
                }
            }
        }
    """
    )

    fun `test rename module member via import`() = doTest(
        "renamed_create", """
        address 0x1 {
            module Transaction {
                public fun create() {}
            }
            
            module M {
                use 0x1::Transaction;
    
                fun main() {
                    let a = Transaction::/*caret*/create();
                }
            }
        }
    """, """
        address 0x1 {
            module Transaction {
                public fun renamed_create() {}
            }
            
            module M {
                use 0x1::Transaction;
    
                fun main() {
                    let a = Transaction::renamed_create();
                }
            }
        }
    """
    )

    fun `test rename schema field with shorthand`() = doTest(
        "root_account", """
    module 0x1::M {
        spec schema Schema {
            /*caret*/account: address;
        }
        spec module {
            let account = @0x1;
            include Schema { account };
        }
    }    
    """, """
    module 0x1::M {
        spec schema Schema {
            root_account: address;
        }
        spec module {
            let account = @0x1;
            include Schema { root_account: account };
        }
    }    
    """
    )

    fun `test rename schema expr binding with shorthand`() = doTest(
        "root_account", """
    module 0x1::M {
        spec schema Schema {
            account: address;
        }
        spec module {
            let /*caret*/account = @0x1;
            include Schema { account };
        }
    }    
    """, """
    module 0x1::M {
        spec schema Schema {
            account: address;
        }
        spec module {
            let root_account = @0x1;
            include Schema { account: root_account };
        }
    }    
    """
    )

    fun `test Self is not renamed for module`() = doTest(
        "RenamedModule", """
    module 0x1::/*caret*/Module {
        struct S {}
    }        
    module 0x1::Main {
        use 0x1::Module::{Self, S};
    }
    """, """
    module 0x1::RenamedModule {
        struct S {}
    }        
    module 0x1::Main {
        use 0x1::RenamedModule::{Self, S};
    }
    """
    )

    fun `test file name is changed if only module in file`() =
        doTestFilename(
            "RenamedModule", "Module.move", "RenamedModule.move", """
    module 0x1::/*caret*/Module {}        
        """, """
    module 0x1::RenamedModule {}        
        """
        )

    fun `test rename signer in test function`() = doTest("new_acc", """
    module 0x1::M {
        #[test(acc = @0x1)]
        fun call(/*caret*/acc: signer) {}
    }    
    """, """
    module 0x1::M {
        #[test(new_acc = @0x1)]
        fun call(new_acc: signer) {}
    }    
    """)

    fun `test file rename renames module if only one and same name`() {
        val fileText = """
    module 0x1::Main { use 0x1::Main; }                
        """
        InlineFile(fileText, "Main.move")

        myFixture.renameElement(myFixture.file, "MyMain.move")

        val afterText = """
    module 0x1::MyMain { use 0x1::MyMain; }                
        """
        myFixture.checkResult(afterText)
    }

    fun `test rename function argument in specs`() = doTest("new_name", """
    module 0x1::main {
        fun call(/*caret*/old_name: bool) {}
        spec call {
            assert old_name;
        }
    }    
    """, """
    module 0x1::main {
        fun call(new_name: bool) {}
        spec call {
            assert new_name;
        }
    }    
    """)

    fun `test rename function argument in module specs`() = doTest("new_name", """
    module 0x1::main {
        fun call(/*caret*/old_name: bool) {}
    }
    spec 0x1::main {
        spec call {
            assert old_name;
        }

    }    
    """, """
    module 0x1::main {
        fun call(new_name: bool) {}
    }
    spec 0x1::main {
        spec call {
            assert new_name;
        }

    }    
    """)


    fun `test rename shorthand into non-shorthand`() = doTest("new_val", """
    module 0x1::main {
        struct S { val: u8 }
        fun get_s(): S { S { val: 10 } }
        fun main() {
            let S { val } = get_s();
            /*caret*/val;
        }
    }        
    """, """
    module 0x1::main {
        struct S { val: u8 }
        fun get_s(): S { S { val: 10 } }
        fun main() {
            let S { val: new_val } = get_s();
            new_val;
        }
    }        
    """)

    fun `test rename parameter field init shorthand replaced`() = doTest("self", """
        module 0x1::main {
            struct Option<T> { vec: vector<T> };
            public fun from_vec<Element>(/*caret*/vec: vector<Element>): Option<Element> {
                assert!(vec.length() <= 1, EOPTION_VEC_TOO_LONG);
                Option { vec }
            }
        }        
    """, """
        module 0x1::main {
            struct Option<T> { vec: vector<T> };
            public fun from_vec<Element>(self: vector<Element>): Option<Element> {
                assert!(self.length() <= 1, EOPTION_VEC_TOO_LONG);
                Option { vec: self }
            }
        }        
    """
    )

    private fun doTest(
        newName: String,
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) {
        InlineFile(before).withCaret()
        val element = myFixture.elementAtCaret
        myFixture.renameElement(element, newName, false, false)
        myFixture.checkResult(after)
    }

    private fun doTestFilename(
        newName: String,
        beforeFileName: String,
        afterFileName: String,
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) {
        InlineFile(before, name = beforeFileName).withCaret()
        val element = myFixture.elementAtCaret
        myFixture.renameElement(element, newName, false, false)
        myFixture.checkResult(after)

        val newFileName = myFixture.file.name
        check(newFileName == afterFileName) { "New filename `$newFileName` != `$afterFileName`" }
    }
}
