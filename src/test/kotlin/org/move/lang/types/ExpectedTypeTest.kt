package org.move.lang.types

import org.move.lang.core.psi.MvPathExpr
import org.move.utils.tests.types.TypificationTestCase

class ExpectedTypeTest : TypificationTestCase() {
    fun `test function parameter primitive type`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun call(a: u8) {}
        fun main() {
            call(my_ref);
                //^ u8
        }
    }    
    """
    )

    fun `test function parameter generic explicit type`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun call<T>(a: T) {}
        fun main() {
            call<u8>(my_ref);
                    //^ u8
        }
    }    
    """
    )

    fun `test function parameter gets type from first parameter`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun call<T>(a: T, b: T) {}
        fun main() {
            call(1u8, my_ref);
                     //^ u8
        }
    }    
    """
    )

    fun `test unknown if too many parameters`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun call<T>(a: T, b: T) {}
        fun main() {
            call(1u8, my_ref, my_ref2);
                            //^ <unknown>
        }
    }    
    """
    )

    fun `test inferred correctly if not enough parameters`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun call<T>(a: T, b: T, c: T) {}
        fun main() {
            call(1u8, my_ref, );
                     //^ u8
        }
    }    
    """
    )

    fun `test inferred from return type`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun identity<T>(a: T): T { a }
        fun main() {
            let a: u8 = identity( );
                               //^ u8
        }
    }    
    """
    )

    fun `test let statement initializer no pattern explicit type`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun main() {
            let a: u8 = my_ref;
                       //^ u8
        }
    }    
    """
    )

//    fun `test let statement struct pattern`() = testExpectedTyExpr(
//        """
//    module 0x1::Main {
//        struct S { val: u8 }
//        fun main() {
//            let S { val } = my_ref;
//                            //^ 0x1::Main::S
//        }
//    }
//    """
//    )

    fun `test let statement struct pattern field explicit type`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        struct S<Type> { val: Type }
        fun main() {
            let val: S<u8> = S { val: my_ref };
                                      //^ u8
        }
    }    
    """
    )

//    fun `test let statement struct pattern field path type`() = testExpectedTyExpr(
//        """
//    module 0x1::Main {
//        struct S<Type> { val: Type }
//        fun main() {
//            let S<u8> { val } = S { val: my_ref };
//                                         //^ u8
//        }
//    }
//    """
//    )

    fun `test struct field literal`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        struct S { val: u8 }
        fun main() {
            S { val: my_ref };
                    //^ u8
        }
    }    
    """
    )

    fun `test struct field literal with generic`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        struct S<Type> { val1: Type }
        fun main() {
            S<u8> { val1: my_ref };
                         //^ u8
        }
    }    
    """
    )

    fun `test struct field literal with generic inferred from outer context`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        struct S<Type> { val1: Type, val2: Type }
        fun main() {
            let a = 1u8;
            S { val1: a, val2: my_ref };
                              //^ u8
        }
    }    
    """
    )

    fun `test unknown if inside other expr`() = testExpectedTyExpr(
        """
    module 0x1::Main {
        fun call() {
            let a: u8 = 1 + my_ref;
                           //^ <unknown>
        }
    }    
    """
    )

    fun `test borrow type`() = testExpectedType<MvPathExpr>(
        """
    module 0x1::main {
        struct LiquidityPool {}
        fun call(pool: &LiquidityPool) {}
        fun main() {
            call(&myref);
                 //^ 0x1::main::LiquidityPool
        }
    }    
    """
    )

    fun `test borrow mut type`() = testExpectedType<MvPathExpr>(
        """
    module 0x1::main {
        struct LiquidityPool {}
        fun call(pool: &mut LiquidityPool) {}
        fun main() {
            call(&mut myref);
                     //^ 0x1::main::LiquidityPool
        }
    }    
    """
    )

    fun `test type argument type no abilities`() = testExpectedTyType(
        """
    module 0x1::main {
        struct Struct<T> {}
        fun main() {
            Struct<S> {};
                 //^ ?T()
        }
    }        
    """
    )

    fun `test type argument type with ability`() = testExpectedTyType(
        """
    module 0x1::main {
        fun main() {
            borrow_global<S>();
                        //^ ?T(key)
        }
    }        
    """
    )

    fun `test vector type argument type`() = testExpectedTyType(
        """
    module 0x1::main {
        fun main() {
            vector<S>[];
                 //^ ?_()
        }
    }        
    """
    )
    
    fun `test if block`() = testExpectedTyExpr("""
        module 0x1::main {
            fun main(): u8 {
                if (true) { my_ref } else { my_ref }
                           //^ u8
            }
        }                
    """)

    fun `test else block`() = testExpectedTyExpr("""
        module 0x1::main {
            fun main(): u8 {
                if (true) { my_ref } else { my_ref }
                                           //^ u8
            }
        }                
    """)
}
