package org.move.lang.stubs

import org.move.utils.tests.MoveStubTestCase

class StubTest : MoveStubTestCase() {
    fun `test module`() = doTest("""
        module M {}
    """, """
        PsiFileStubImpl
          MODULE_DEF:MoveModuleDefStub
    """)

    fun `test module in address`() = doTest("""
        address 0x0 {
            module M {
                fun main() {}
            }
        }
    """, """
        PsiFileStubImpl
          MODULE_DEF:MoveModuleDefStub
    """)

//    fun `test literal is not stubbed inside function statement`() = doTest("""
//        script {
//            fun main() { 10; }
//        }
//    """, """
//        PsiFileStubImpl
//          FUNCTION_DEF:MoveFunctionDefStub
//            FUNCTION_PARAMETER_LIST:PlaceholderStub
//    """)
//
//    fun `test expression is not stubbed inside function statement`() = doTest("""
//        script {
//            fun main() { 2 + 2; }
//        }
//    """, """
//        PsiFileStubImpl
//          FUNCTION_DEF:MoveFunctionDefStub
//            FUNCTION_PARAMETER_LIST:PlaceholderStub
//    """)
}