package org.move.lang.stubs

//import com.intellij.psi.impl.DebugUtil
//import com.intellij.psi.stubs.StubTreeLoader
//import org.intellij.lang.annotations.Language
//import org.move.utils.tests.MoveTestCase
//import org.move.utils.tests.fileTreeFromText

//class StubTest: MoveTestCase() {
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
//
//    private fun doTest(@Language("Move") code: String, expectedStubText: String) {
//        val fileName = "main.move"
//        fileTreeFromText("//- $fileName\n$code").create()
//        val vFile = myFixture.findFileInTempDir(fileName)
//        val stubTree = StubTreeLoader.getInstance().readFromVFile(project, vFile) ?: error("Stub tree is null")
//        val stubText = DebugUtil.stubTreeToString(stubTree.root)
//        assertEquals(expectedStubText.trimIndent() + "\n", stubText)
//    }
//}