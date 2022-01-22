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
}
