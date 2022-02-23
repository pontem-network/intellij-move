package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class ExpressionTypeInferenceTest: TypificationTestCase() {
    fun `test bool`() = testExpr("""
    module 0x1::M {
        fun main() {
            let b = false;
            b;
          //^ bool
        }
    }
    """)

    fun `test function parameter`() = testExpr("""
    module 0x1::M {
        fun main(a: u8) {
            let b = a;
            b;
          //^ u8  
        }
    }
    """)

    fun `test function call`() = testExpr(
        """
    module 0x1::M {
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
    module 0x1::M {
        fun main<F>(a: F) {
            let b = a;
            b;
          //^ F 
        }
    }    
    """)

    fun `test const type`() = testExpr("""
    module 0x1::M {
        const NUM: u8 = 1;
        
        fun main() {
            NUM;
          //^ u8  
        }
    }    
    """)

    fun `test function parameter constraint`() = testExpr("""
    module 0x1::M {
        fun main<Coin: copy + store>(coin: Coin) {
            coin;
          //^ Coin
        }
    }    
    """)

    fun `test tuple type unpacking`() = testExpr("""
    module 0x1::M {
        fun call(): (u8, u8) { (1, 1) }
        fun m() {
            let (a, b) = call();
            a; 
          //^ u8  
        }
    }    
    """)

    fun `test struct type unpacking`() = testExpr("""
    module 0x1::M {
        struct S { val: u8 }
        fun m() {
            let S { val: myval } = S { val: 1 };
            myval;
          //^ u8
        }
    }
    """)

    fun `test struct type unpacking shorthand`() = testExpr("""
    module 0x1::M {
        struct S { val: u8 }
        fun m() {
            let S { val } = S { val: 1 };
            val;
          //^ u8
        }
    }
    """)

    fun `test let inference in msl`() = testExpr("""
    module 0x1::M {
        spec module {
            let post a = 1;
            a;
          //^ num 
        }
    }    
    """)
}
