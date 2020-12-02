package org.move.utils.tests

import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.stubs.StubTreeLoader
import org.intellij.lang.annotations.Language

open class MoveStubTestCase : MoveTestBase() {
    fun doTest(@Language("Move") code: String, expectedStubText: String) {
        val fileName = "main.move"
        fileTreeFromText("//- $fileName\n$code").create()
        val vFile = myFixture.findFileInTempDir(fileName)
        val stubTree =
            StubTreeLoader.getInstance().readFromVFile(project, vFile) ?: error("Stub tree is null")
        val stubText = DebugUtil.stubTreeToString(stubTree.root)
        assertEquals(expectedStubText.trimIndent() + "\n", stubText)
    }
}
