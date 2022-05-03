package org.move.cli.runconfig

import com.intellij.psi.PsiElement
import org.move.cli.runconfig.producers.TestRunConfigurationProducer
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.openapiext.toPsiDirectory
import org.move.utils.tests.RunConfigurationProducerTestBase

class TestsConfigurationProducerTest : RunConfigurationProducerTestBase("test") {
    fun `test test run for function`() {
        testProject {
            namedMoveToml("MyPackage")
            tests {
                move(
                    "MoveTests.move", """
            #[test_only]
            module 0x1::MoveTests {
                #[test]
                fun /*caret*/test_add() {
                    1 + 1;
                }
                #[test]
                fun test_mul() {
                    1 * 1;
                }
            }
            """
                )
            }
        }
        checkOnElement<MvFunction>()

        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
    }

    fun `test no test run if no test functions`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                #[test_only]
                module 0x1::/*caret*/M {
                    fun call() {}
                }
                """
                )
            }
        }
        checkNoConfigurationOnElement<MvModule>()
    }

    fun `test test run for module with test functions inside sources`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                #[test_only]
                module 0x1::/*caret*/Main {
                    #[test]
                    fun test_some_action() {}
                }
                """
                )
            }
        }
        checkOnElement<MvModule>()
    }

    fun `test run tests for sources directory`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                module 0x1::Main {
                    #[test]
                    fun test_main() {/*caret*/}
                }    
                """
                )
            }
        }
        val sourcesDir = this.testProject?.rootDirectory
            ?.findChild("sources")?.toPsiDirectory(this.project) ?: error("No sources directory")
        checkOnFsItem(sourcesDir)
    }

    fun `test run tests for move package`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                module 0x1::Main {
                    #[test]
                    fun test_main() {/*caret*/}
                }    
                """
                )
            }
        }
        val sourcesDir = this.testProject?.rootDirectory
            ?.findChild("sources")?.toPsiDirectory(this.project) ?: error("No sources directory")
        val mainFile = sourcesDir.findFile("main.move") ?: error("No file")
        checkOnFsItem(mainFile)
    }

//    fun `test no configuration on sources if no test function`() {
//        testProject {
//            namedMoveToml("MyPackage")
//            sources {
//                move(
//                    "main.move", """
//                module 0x1::Main {
//                    fun test_main() {/*caret*/}
//                }
//                """
//                )
//            }
//        }
//        val sourcesDir =
//            this.testProject?.rootDirectory?.toPsiDirectory(this.project) ?: error("No sources directory")
//        checkNoConfigurationOnFsItem(sourcesDir)
//    }
}
