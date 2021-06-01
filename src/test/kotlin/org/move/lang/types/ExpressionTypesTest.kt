package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class ExpressionTypesTest: TypificationTestCase() {
    fun `test borrow expr`() = testExpr("""
    module M {
        fun main(s: signer) {
            &s;
          //^ &signer 
        }
    }    
    """)

    fun `test mutable borrow expr`() = testExpr("""
    module M {
        fun main(s: signer) {
            &mut s;
          //^ &mut signer 
        }
    }    
    """)

    fun `test deref expr`() = testExpr("""
    module M {
        fun main(s: &u8) {
            *s;
          //^ u8 
        }
    }    
    """)
}
