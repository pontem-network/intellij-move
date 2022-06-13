package org.move.cli.runconfig

import com.intellij.psi.PsiElement
import org.move.cli.runconfig.producers.TestCommandConfigurationProducer
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.utils.tests.RunConfigurationProducerTestBase

class AptosCommandConfigurationProducerTest : RunConfigurationProducerTestBase("test") {
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
        doTestRemembersContext(TestCommandConfigurationProducer(), ctx1, ctx2)
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
        val sourcesDir = findPsiDirectory("sources")
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
        val sourcesDir = findPsiDirectory("sources")
        checkOnFsItem(sourcesDir)
    }

    fun `test run tests for file with one module`() {
        testProject {
            dir("mypackage") {
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
        }
        val mainFile = findPsiFile("mypackage/sources/main.move")
        checkOnFsItem(mainFile)
    }

    fun `test run tests for function if inside non test only module`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                module 0x1::Main {
                    #[test]
                    fun /*caret*/test_main() {}
                }
                """
                )
            }
        }
        checkOnElement<MvFunction>()
    }

    fun `test publish module`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                module 0x1::/*caret*/Main {}                    
                """
                )
            }
        }
        checkOnElement<MvModule>()
    }

    fun `test publish and test for module with test function`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                module 0x1::/*caret*/Main {
                    #[test]
                    fun test_a() {
                    }
                }                    
                """
                )
            }
        }
        checkOnElement<MvModule>()
    }

    fun `test no publish action if test_only`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                #[test_only]    
                module 0x1::/*caret*/Main {}                    
                """
                )
            }
        }
        checkNoConfigurationOnElement<MvModule>()
    }
}
