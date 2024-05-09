package org.move.cli.externalLinter

import org.intellij.lang.annotations.Language
import org.move.ide.annotator.RsExternalLinterUtils
import org.move.utils.tests.WithAptosCliTestBase

class MvExternalLinterPassTest: WithAptosCliTestBase() {

    override fun setUp() {
        super.setUp()
        project.externalLinterSettings.modifyTemporary(testRootDisposable) { it.runOnTheFly = true }
    }

    fun `test no errors if everything is ok`() = doTest(
        """
        module 0x1::main {
            fun main() {/*caret*/
            }
        }
    """
    )

    fun `test type error`() = doTest(
        """
        module 0x1::main {
            fun main() {
                1 <error descr="${RsExternalLinterUtils.TEST_MESSAGE}">+</error> true/*caret*/
            }
        }
    """
    )

//    fun `test highlights errors in tests`() = doTest("""
//        #[test_only]
//        module 0x1::main {
//            #[test]
//            fun main() {
//                1 <error descr="${RsExternalLinterUtils.TEST_MESSAGE}">+</error> true/*caret*/
//            }
//        }
//    """)

    private fun doTest(
        @Language("Move") mainMove: String,
//        externalLinter: ExternalLinter = ExternalLinter.DEFAULT
    ) {
//        project.externalLinterSettings.modifyTemporary(testRootDisposable) { it.tool = externalLinter }
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage"
            version = "0.1.0"
            """
            )
            sources {
                main(mainMove)
            }
        }
        myFixture.openFileInEditor(cargoProjectDirectory.findFileByRelativePath("sources/main.move")!!)
        myFixture.checkHighlighting()
    }
}