package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class SpecsCompletionTest: CompletionTestCase() {
    fun `test function name in spec block label`() = doSingleCompletion("""
    module 0x1::M {
        fun call() {}
        spec ca/*caret*/
    }    
    """, """
    module 0x1::M {
        fun call() {}
        spec call /*caret*/
    }    
    """)

    fun `test struct name in spec block label`() = doSingleCompletion("""
    module 0x1::M {
        struct Struct {}
        spec Str/*caret*/
    }    
    """, """
    module 0x1::M {
        struct Struct {}
        spec Struct /*caret*/
    }    
    """)

    fun `test let bindings inside specs`() = doSingleCompletion("""
    module 0x1::M {
        fun call() {}
        spec call {
            let account = 1;
            old(acc/*caret*/)
        }
    }    
    """, """
    module 0x1::M {
        fun call() {}
        spec call {
            let account = 1;
            old(account/*caret*/)
        }
    }    
    """)

    fun `test function parameters inside specs`() = doSingleCompletion("""
    module 0x1::M {
        fun call(account: &signer) {}
        spec call {
            old(acc/*caret*/)
        }
    }    
    """, """
    module 0x1::M {
        fun call(account: &signer) {}
        spec call {
            old(account/*caret*/)
        }
    }    
    """)

    fun `test schema parameters`() = doSingleCompletion("""
    module 0x1::M {
        spec schema Schema {
            account: address;
            old(acc/*caret*/)
        }
    }    
    """, """
    module 0x1::M {
        spec schema Schema {
            account: address;
            old(account/*caret*/)
        }
    }    
    """)

    fun `test schema let statements`() = doSingleCompletion("""
    module 0x1::M {
        spec schema Schema {
            let account = @0x1;
            old(acc/*caret*/)
        }
    }    
    """, """
    module 0x1::M {
        spec schema Schema {
            let account = @0x1;
            old(account/*caret*/)
        }
    }    
    """)

    fun `test schema names in include`() = doSingleCompletion("""
    module 0x1::M {
        spec module {
            include Sch/*caret*/
        }
        spec schema Schema {}
    }    
    """, """
    module 0x1::M {
        spec module {
            include Schema/*caret*/
        }
        spec schema Schema {}
    }    
    """)

    fun `test schema names from other module`() = doSingleCompletion("""
    module 0x1::SS {
        spec schema Schema {}
    }    
    module 0x1::M {
        use 0x1::SS;
        
        spec module {
            include SS::Sc/*caret*/
        }
    }    
    """, """
    module 0x1::SS {
        spec schema Schema {}
    }    
    module 0x1::M {
        use 0x1::SS;
        
        spec module {
            include SS::Schema/*caret*/
        }
    }    
    """)

    fun `test function generic type`() = doSingleCompletion("""
    module 0x1::M {
        fun call<Type>() {}
        spec call {
            let a: Ty/*caret*/ = 1;
        }
    }    
    """, """
    module 0x1::M {
        fun call<Type>() {}
        spec call {
            let a: Type/*caret*/ = 1;
        }
    }    
    """)

    fun `test schema generic type`() = doSingleCompletion("""
    module 0x1::M {
        spec schema Schema<Type> {
            let a: Ty/*caret*/ = 1;
        }
    }    
    """, """
    module 0x1::M {
        spec schema Schema<Type> {
            let a: Type/*caret*/ = 1;
        }
    }    
    """)

    fun `test complete consts from another module`() = doSingleCompletion("""
    module 0x1::M {
        const MY_CONST: u8 = 1;
              //X
    }    
    module 0x1::M2 {
        use 0x1::M;
        spec module {
            M::MY_/*caret*/;
        }
    }        
    """, """
    module 0x1::M {
        const MY_CONST: u8 = 1;
              //X
    }    
    module 0x1::M2 {
        use 0x1::M;
        spec module {
            M::MY_CONST/*caret*/;
        }
    }        
    """)

    fun `test include schema angle brackets completion`() = doSingleCompletion("""
    module 0x1::M {
        fun call() {}
        spec call {
            include MySche/*caret*/
        }
        spec schema MySchema<Type> {}
    }    
    """, """
    module 0x1::M {
        fun call() {}
        spec call {
            include MySchema</*caret*/>
        }
        spec schema MySchema<Type> {}
    }    
    """)

    fun `test autocomplete fields in include schema`() = doSingleCompletion("""
    module 0x1::M {
        fun call() {}
        spec call {
            include MySchema { ad/*caret*/ };
        }
        spec schema MySchema {
            addr: address;
        }
    }
    """, """
    module 0x1::M {
        fun call() {}
        spec call {
            include MySchema { addr/*caret*/ };
        }
        spec schema MySchema {
            addr: address;
        }
    }
    """)

    fun `test autocomplete fields in include schema if full field name is provided`() = doSingleCompletion("""
    module 0x1::M {
        fun call() {}
        spec call {
            include MySchema { addr/*caret*/ };
        }
        spec schema MySchema {
            addr: address;
        }
    }
    """, """
    module 0x1::M {
        fun call() {}
        spec call {
            include MySchema { addr/*caret*/ };
        }
        spec schema MySchema {
            addr: address;
        }
    }
    """)

    fun `test no completion if field already used`() = checkNoCompletion("""
    module 0x1::M {
        fun call() {}
        spec call {
            include MySchema { addr, ad/*caret*/ };
        }
        spec schema MySchema {
            addr: address;
        }
    }
    """)

    fun `test no completion if field already used at the end`() = checkNoCompletion("""
    module 0x1::M {
        fun call() {}
        spec call {
            include MySchema { ad/*caret*/, addr };
        }
        spec schema MySchema {
            addr: address;
        }
    }
    """)

    fun `test aborts_if false`() = doSingleCompletion("""
        module 0x1::m {
            fun call() {}
            spec call {
                aborts_if fa/*caret*/;
            }
        }        
    """, """
        module 0x1::m {
            fun call() {}
            spec call {
                aborts_if false/*caret*/;
            }
        }        
    """)
}
