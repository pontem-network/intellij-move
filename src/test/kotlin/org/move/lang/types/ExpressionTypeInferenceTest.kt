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

    fun `test vector no specific type`() = testExpr("""
    module 0x1::M {
        native public fun vector_empty<Element>(): vector<Element>;
        native public fun vector_push_back<Element>(v: &mut vector<Element>, e: Element);
        fun call() {
            let v = vector_empty();
            v;
          //^ vector<?Element>  
        }
    }        
    """)

    fun `test vector inferred type`() = testExpr("""
    module 0x1::M {
        native public fun vector_empty<El>(): vector<El>;
        native public fun vector_push_back<Element>(v: &mut vector<Element>, e: Element);
        fun call() {
            let v = vector_empty();
            vector_push_back(&mut v, 1u8);
            v;
          //^ vector<u8>  
        }
    }        
    """)

    fun `test vector inferred type generic parameters`() = testExpr("""
    module 0x1::M {
        native public fun vector_empty<El>(): vector<El>;
        native public fun vector_push_back<Element>(v: &mut vector<Element>, e: Element);
        fun call() {
            let v = vector_empty();
            vector_push_back<u8>(&mut v);
            v;
          //^ vector<u8>  
        }
    }        
    """)

    fun `test infer type with struct literal`() = testExpr("""
    module 0x1::M {
        struct S { vec: vector<u8> }
        native public fun vector_empty<El>(): vector<El>;
        fun call() {
            let v = vector_empty();
            S { vec: v };
            v;
          //^ vector<u8>  
        }
    }        
    """)

    fun `test integer type inference from call expr`() = testExpr("""
    module 0x1::M {
        fun add_u8(a: u8, b: u8): u8 { a + b }
        fun call() {
            let a = 0;
            let b = 1;
            add_u8(a, b);
            a;
          //^ u8  
        }
    }    
    """)

    fun `test integer type inference with return type`() = testExpr("""
    module 0x1::M {
        fun get_u8(): u8 {
            let a = 1;
            a
          //^ u8  
        }
    }    
    """)
}
