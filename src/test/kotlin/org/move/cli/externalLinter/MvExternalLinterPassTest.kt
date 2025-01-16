package org.move.cli.externalLinter

import org.intellij.lang.annotations.Language
import org.move.ide.annotator.externalLinter.RsExternalLinterUtils.APTOS_TEST_MESSAGE
import org.move.utils.tests.MoveV2
import org.move.utils.tests.WithAdvancedSetting
import org.move.utils.tests.WithAptosCliTestBase
import kotlin.io.path.Path

class MvExternalLinterPassTest: WithAptosCliTestBase(
//    Path(System.getProperty("user.home"))
//        .resolve("code/aptos-core/target/release/aptos")
) {

    override fun setUp() {
        super.setUp()
        project.externalLinterSettings.modifyTemporary(testRootDisposable) { it.runOnTheFly = true }
    }

    @MoveV2
    fun `test no errors if everything is ok`() = doTest(
        """
        module 0x1::main {
            fun main() {/*caret*/
            }
        }
    """
    )

    @MoveV2
    fun `test type error`() = doTest(
        """
        module 0x1::main {
            fun main() {
                1 + <error descr="$APTOS_TEST_MESSAGE">/*caret*/true</error>;
            }
        }
    """, externalLinter = ExternalLinter.LINTER
    )

//    @WithAdvancedSetting("org.move.aptos.compile.message.json", true)
//    @MoveV2
//    fun `test type error with json message format`() = doTest(
//        """
//        module 0x1::main {
//            fun main() {
//                <weak_warning descr="$APTOS_TEST_MESSAGE">/*caret*/*&1</weak_warning>;
//            }
//        }
//    """, externalLinter = ExternalLinter.LINTER
//    )

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
        externalLinter: ExternalLinter = ExternalLinter.DEFAULT
    ) {
        project.externalLinterSettings.modifyTemporary(testRootDisposable) { it.tool = externalLinter }
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