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

    fun `test borrow_global_mut returns reference to type`() = testExpr(
        """
    module 0x1::M {
        struct MyToken has key {}
        fun main() acquires MyToken {
            borrow_global_mut<MyToken>(@0x1);
          //^ &mut 0x1::M::MyToken  
        }
    }    
    """
    )

    fun `test primitive type field reference`() = testExpr(
        """
    module 0x1::M {
        struct MyToken has key { val: u8 }
        fun main() acquires MyToken {
            (borrow_global_mut<MyToken>(@0x1).val);
          //^ u8
        }
    }    
    """
    )

    fun `test type field reference for struct`() = testExpr(
        """
    module 0x1::M {
        struct Val has store {}
        struct MyToken has key { val: Val }
        fun main() acquires MyToken {
            (borrow_global_mut<MyToken>(@0x1).val);
          //^ 0x1::M::Val
        }
    }    
    """
    )

    fun `test parametrized struct from literal`() = testExpr("""
    module 0x1::M {
        struct MyToken<Num> has key {}
        fun main() {
            MyToken<u8> {};
          //^ 0x1::M::MyToken<u8>
        }
    }    
    """)

    fun `test parametrized struct from call expr`() = testExpr("""
    module 0x1::M {
        struct MyToken<Num> has key {}
        fun call<Token>(): Token {}
        fun main() {
            call<MyToken<u8>>()
          //^ 0x1::M::MyToken<u8>
        }
    }    
    """)

    fun `test struct field reference with generic field`() = testExpr(
        """
    module 0x1::M {
        struct MyToken<Num> has key { val: Num }
        fun call<Token>(): Token {}
        fun main() acquires MyToken {
            (borrow_global_mut<MyToken<u8>>(@0x1).val);
          //^ u8
        }
    }    
    """
    )

    fun `test return type of generic function parametrized by the vector of types`() = testExpr("""
    module M {
        native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
        
        fun m() {
            let a: vector<u8>;
            let b = borrow(&a, 0);
            b;
          //^ &u8 
        }
    }    
    """)
}
