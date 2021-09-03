package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class ExpressionTypeInferenceTest: TypificationTestCase() {
    fun `test bool`() = testExpr("""
    module M {
        fun main() {
            let b = false;
            b;
          //^ bool
        }
    }
    """)

    fun `test function parameter`() = testExpr("""
    module M {
        fun main(a: u8) {
            let b = a;
            b;
          //^ u8  
        }
    }
    """)

    fun `test function call`() = testExpr(
        """
    module M {
        fun call(): u8 { 1 }
        fun main() {
            let b = call();
            b;
          //^ u8  
        }
    }
    """
    )

    fun `test type parameter`() = testExpr("""
    module M {
        fun main<F>(a: F) {
            let b = a;
            b;
          //^ F 
        }
    }    
    """)

    fun `test const type`() = testExpr("""
    module M {
        const NUM: u8 = 1;
        
        fun main() {
            NUM;
          //^ u8  
        }
    }    
    """)
}
