package org.move.lang.types

import org.intellij.lang.annotations.Language
import org.move.ide.presentation.text
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvMethodCall
import org.move.lang.core.psi.ext.MvCallable
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.utils.tests.MoveV2
import org.move.utils.tests.InlineFile
import org.move.utils.tests.base.findElementAndDataInEditor
import org.move.utils.tests.types.TypificationTestCase

class CallableTypeTest: TypificationTestCase() {
    @MoveV2()
    fun `test infer method type`() = testMethodType("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T, U>(self: &S<T>, param: U): U {
                param
            }
            fun main(s: S<u8>) {
                s.receiver(1);
                   //^ fn(&S<u8>, integer) -> integer
            }
        }        
    """)

    @MoveV2()
    fun `test infer method type not enough parameters`() = testMethodType("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T, U, Z>(self: &S<T>, param: U): Z {
                param
            }
            fun main(s: S<u8>) {
                s.receiver();
                   //^ fn(&S<u8>, <unknown>) -> ?Z
            }
        }        
    """)

    @MoveV2()
    fun `test infer method type cannot autoborrow unknown function`() = testMethodType("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T, U>(self: &mut S<T>, param: U): U {
                param
            }
            fun main(s: &S<u8>) {
                s.receiver(1);
                   //^ fn(<unknown>, <unknown>) -> <unknown>
            }
        }        
    """)

    fun `test function with explicit integer parameter callable type`() = testFunctionType("""
        module 0x1::m {
            fun call<T>(t: T): T { t }
            fun main() {
                call<u8>(1);
              //^ fn(u8) -> u8
            }
        }        
    """)

    private fun testFunctionType(@Language("Move") code: String) = testCallableType<MvCallExpr>(code)
    private fun testMethodType(@Language("Move") code: String) = testCallableType<MvMethodCall>(code)

    private inline fun <reified T: MvCallable> testCallableType(@Language("Move") code: String) {
        InlineFile(myFixture, code, "main.move")
        val (callable, data) = myFixture.findElementAndDataInEditor<T>()
        val expectedType = data.trim()

        val msl = callable.isMsl()
        val inference = callable.inference(msl) ?: error("No inference at caret element")

        val actualType = inference.getCallableType(callable)?.text() ?: "null"
        check(actualType == expectedType) {
            "Type mismatch. \n" +
                    "    expected: $expectedType, \n    found: $actualType"
        }
    }
}