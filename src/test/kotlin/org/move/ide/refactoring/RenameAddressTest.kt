package org.move.ide.refactoring

import com.intellij.openapi.project.rootManager
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveHeavyTestBase
import org.move.utils.tests.fileTreeFromText

class RenameAddressTest : MoveHeavyTestBase() {
    fun `test rename from move correctly replaces Std in toml`() = doTest(
        "NoStd", """
        //- Move.toml
        [addresses]
        Std = "0x1"
        #X
        //- sources/main.move
        module /*caret*/Std::Module {}
    """, """
        [addresses]
        NoStd = "0x1"
        #X
    """.trimIndent()
    )

    fun `test rename from toml correctly replaces Std in move`() = doTest(
        "NoStd", """
        //- Move.toml
        [addresses]
        Std = "0x1"

        //- sources/main.move
        module /*caret*/Std::Module {}
             //X
    """, """
        module NoStd::Module {}
             //X
    """.trimIndent()
    )

    private fun doTest(
        newName: String,
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) {
        val fileTree = fileTreeFromText(before)
        val rootDirectory = myModule.rootManager.contentRoots.first()
        val testProject = fileTree.prepareTestProject(myFixture.project, rootDirectory)
        myFixture.configureFromFileWithCaret(testProject)

        val element = myFixture.elementAtCaret
        myFixture.renameElement(element, newName, false, false)
        myFixture.checkResult(
            testProject.fileWithNamedElement, after, true
        )
    }
}
