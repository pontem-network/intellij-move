package org.move.ide.docs

//import org.move.utils.tests.MvDocumentationProviderProjectTestCase

//class MvNamedModulePathDocumentationTest : MvDocumentationProviderProjectTestCase() {
//    fun `test value of named address accessible from documentation`() = doTestByFileTree(
//        {
//            moveToml(
//                """
//    [package]
//    name = "UserInfo"
//    version = "0.1.0"
//
//    [addresses]
//    Std = "0x42"
//            """
//            )
//            sources {
//                move(
//                    "main.move", """
//    module Std::Module {}
//          //^
//                """
//                )
//            }
//        },
//        "Std = \"0x42\"", block = MvDocumentationProvider::generateDoc
//    )
//}
