package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class TypeSubstitutionTest: TypificationTestCase() {
    fun `test return type of callable`() = testExpr("""
    module M {
        fun call<R>(): R {}
        fun main() {
            call<u8>()
          //^ u8  
        }
    }    
    """)

    fun `test borrow_global_mut returns reference to type`() = testExpr("""
    module M {
        fun main() {
            borrow_global_mut<u8>(0x1)
          //^ &mut u8  
        }
    }    
    """)

    fun `test struct field reference`() = testExpr("""
    module M {
        struct MyToken has key { val: u8 }
        fun main() {
            (borrow_global_mut<MyToken>(0x1).val)
          //^ &mut u8
        }
    }    
    """)

    fun `test parametrized struct from literal`() = testExpr("""
    module M {
        struct MyToken<Num> has key {}
        fun main() {
            MyToken<u8> {}
          //^ 0x1::M::MyToken<u8>
        }
    }    
    """)

    fun `test parametrized struct from call expr`() = testExpr("""
    module M {
        struct MyToken<Num> has key {}
        fun call<Token>(): Token {}
        fun main() {
            call<MyToken<u8>>()
          //^ 0x1::M::MyToken<u8>
        }
    }    
    """)

    fun `test struct field reference with generic field`() = testExpr("""
    module M {
        struct MyToken<Num> has key { val: Num }
        fun call<Token>(): Token {}
        fun main() {
            (borrow_global_mut<MyToken<u8>>(0x1).val)
          //^ &mut u8
        }
    }    
    """)
}
