package org.move.ide.refactoring

import com.intellij.openapi.project.rootManager
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.TreeBuilder
import org.move.utils.tests.fileTreeFromText

class RenameAddressTest : MvProjectTestBase() {
    fun `test rename from move correctly replaces Std in toml`() = doTest(
        "NoStd", {
            moveToml(
                """
[addresses]
Std = "0x1"
#X
                     """
            )
            sources {
                main(
                    """
module /*caret*/Std::Module {}                    
                """
                )
            }
        }, """
        [addresses]
        NoStd = "0x1"
        #X
    """
    )

    fun `test rename from toml correctly replaces Std in move`() = doTest(
        "NoStd", {
            moveToml(
                """
[addresses]
/*caret*/Std = "0x1"
                     """
            )
            sources {
                main(
                    """
module Std::Module {}                
      //X
                """
                )
            }
        }, """
        module NoStd::Module {}
              //X
    """
    )

    private fun doTest(
        newName: String,
        @Language("Move") code: TreeBuilder,
        @Language("Move") after: String,
    ) {
        val testProject = testProjectFromFileTree(code)
        myFixture.configureFromFileWithCaret(testProject)

        val element = myFixture.elementAtCaret
        myFixture.renameElement(element, newName, false, false)
        myFixture.checkResult(
            testProject.fileWithNamedElement, after.trimIndent(), true
        )
    }
}
