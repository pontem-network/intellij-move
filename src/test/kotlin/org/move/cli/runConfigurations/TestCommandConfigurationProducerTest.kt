package org.move.cli.runConfigurations

import org.move.cli.settings.moveSettings
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.openapiext.toPsiDirectory
import org.move.utils.tests.RunConfigurationProducerTestBase

class TestCommandConfigurationProducerTest: RunConfigurationProducerTestBase("test") {
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
    }

    fun `test test run for function skipping fetch git deps`() {
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
//        this.project.moveSettings
//            .modifyTemporary(this.testRootDisposable) {
//                it.extraTestArgs = "--dev --skip-fetch-latest-git-deps"
//            }
        checkOnElement<MvFunction>()
    }

    fun `test test run for function dumping state on test failure`() {
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
        this.project.moveSettings
            .modifyTemporary(this.testRootDisposable) {
                it.extraTestArgs = "--dev --dump"
            }
        checkOnElement<MvFunction>()
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

    fun `test run tests for move package from root`() {
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
        val sourcesDir = this.rootDirectory?.toPsiDirectory(this.project) ?: error("no root")
        checkOnFsItem(sourcesDir)
    }

    fun `test run tests for move package from tests directory`() {
        testProject {
            namedMoveToml("MyPackage")
            tests {
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
        val sourcesDir = findPsiDirectory("tests")
        checkOnFsItem(sourcesDir)
    }

    fun `test cannot run tests for move package sources`() {
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
        checkNoConfigurationOnFsItem(sourcesDir)
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

    fun `test run tests for module with test function`() {
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

    fun `test run tests for move toml file`() {
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage" # /*caret*/                    
            """
            )
            sources {
                move(
                    "main.move", """
                #[test_only]    
                module 0x1::Main {}                    
                """
                )
            }
        }
        val mainFile = findPsiFile("Move.toml")
        checkOnFsItem(mainFile)
    }

    fun `test no test run on python file inside move project`() {
        testProject {
            namedMoveToml("MyPackage")
            dir("python") {
                file("main.py", """hello/*caret*/""")
            }
            sources {
                move(
                    "main.move", """
                #[test_only]    
                module 0x1::Main {}                    
                """
                )
            }
        }
        val mainFile = findPsiFile("python/main.py")
        checkNoConfigurationOnFsItem(mainFile)
    }

    fun `test run tests for file with multiple module single test module`() {
        testProject {
            dir("mypackage") {
                namedMoveToml("MyPackage")
                sources {
                    move(
                        "main.move", """
                module 0x1::mod1 {}                            
                module 0x1::mod2 {}
                                            
                #[test_only]                            
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

    fun `test test run for compiler v2 without cli flag`() {
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
        project.moveSettings.modifyTemporary(this.testRootDisposable) {
            it.extraTestArgs = "--dev"

        }
        checkOnElement<MvFunction>()
    }

    fun `test test run for compiler v2`() {
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
        project.moveSettings.modifyTemporary(this.testRootDisposable) {
            it.extraTestArgs = "--dev"
            it.enableMove2 = true
        }
        checkOnElement<MvFunction>()
    }
}
