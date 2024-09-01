package org.move.lang.types.compilerV2

import org.move.utils.tests.MoveV2
import org.move.utils.tests.types.TypificationTestCase

@MoveV2()
class ReceiverStyleFunctionsTest: TypificationTestCase() {
    fun `test infer receiver style function type generic self`() = testExpr("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T>(self: S<T>): T {
                self.field
            }
            fun main(s: S<u8>) {
                s.receiver();
                  //^ u8
            }
        }        
    """)

    fun `test infer receiver style function type generic param`() = testExpr("""
        module 0x1::main {
            struct S { field: u8 }
            fun receiver<T>(self: S, p: T): T {
                p
            }
            fun main(s: S) {
                s.receiver(1u8);
                  //^ u8
            }
        }        
    """)

    fun `test method infer parameter type`() = testExpr("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T, U>(self: &S<T>, param: U): U {
                param
            }
            fun main(s: S<u8>) {
                let a = s.receiver(1);
                a;
              //^ integer
            }
        }        
    """)
}