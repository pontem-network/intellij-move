package org.move.ide.refactoring

//class RenameAddressTest : MvProjectTestBase() {
//    fun `test rename from move correctly replaces Std in toml`() = doTest(
//        "NoStd", {
//            moveToml(
//                """
//[addresses]
//Std = "0x1"
//#X
//                     """
//            )
//            sources {
//                main(
//                    """
//module /*caret*/Std::Module {}
//                """
//                )
//            }
//        }, """
//        [addresses]
//        NoStd = "0x1"
//        #X
//    """
//    )
//
//    fun `test rename from toml correctly replaces Std in move`() = doTest(
//        "NoStd", {
//            moveToml(
//                """
//[addresses]
///*caret*/Std = "0x1"
//                     """
//            )
//            sources {
//                main(
//                    """
//module Std::Module {}
//      //X
//                """
//                )
//            }
//        }, """
//        module NoStd::Module {}
//              //X
//    """
//    )
//
//    fun `test rename from move address literal correctly replaces Std in toml`() = doTest(
//        "NoStd", {
//            moveToml(
//                """
//[addresses]
//Std = "0x1"
//#X
//                     """
//            )
//            sources {
//                main(
//                    """
//module 0x1::Module {
//    fun call() {
//        @/*caret*/Std;
//    }
//}
//                """
//                )
//            }
//        }, """
//[addresses]
//NoStd = "0x1"
//#X
//    """
//    )
//
//    private fun doTest(
//        newName: String,
//        @Language("Move") code: TreeBuilder,
//        @Language("Move") after: String,
//    ) {
//        val testProject = testProject(code)
//
//        val element = myFixture.elementAtCaret
//        check(MvRenameProcessor().canProcessElement(element)) {
//            "MvRenameAddressProcessor cannot process element"
//        }
//
//        myFixture.renameElementAtCaretUsingHandler(newName)
//        myFixture.checkResult(
//            testProject.fileWithNamedElement, after.trimIndent(), true
//        )
//    }
//}
