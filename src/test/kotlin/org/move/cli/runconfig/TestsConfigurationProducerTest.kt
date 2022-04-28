package org.move.cli.runconfig

import com.intellij.psi.PsiElement
import org.move.cli.runconfig.producers.TestRunConfigurationProducer
import org.move.cli.settings.ProjectType
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.openapiext.toPsiDirectory
import org.move.utils.tests.RunConfigurationProducerTestBase
import org.move.utils.tests.SettingsProjectType

class TestsConfigurationProducerTest : RunConfigurationProducerTestBase("test") {
    @SettingsProjectType(ProjectType.DOVE)
    fun `test dove test run for function`() {
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage"
            """
            )
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

    @SettingsProjectType(ProjectType.DOVE)
    fun `test dove no test run if no test functions`() {
        testProject {
            moveToml("""""")
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

    @SettingsProjectType(ProjectType.DOVE)
    fun `test dove test run for module with test functions inside sources`() {
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage"    
            """
            )
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

    @SettingsProjectType(ProjectType.APTOS)
    fun `test aptos test run for sources`() {
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

    @SettingsProjectType(ProjectType.APTOS)
    fun `test aptos test run for file`() {
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

//    @SettingsProjectType(ProjectType.APTOS)
//    fun `test aptos no configuration on sources if no test function`() {
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
