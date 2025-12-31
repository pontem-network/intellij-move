package org.move.lang.completion.names.project

import org.move.utils.tests.completion.CompletionProjectTestCase

class ModulesCompletionProjectTest: CompletionProjectTestCase() {

    fun `test complete modules from all the files in imports`() =
        checkContainsCompletionsExact(listOf("M1", "M2")) {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "module.move", """
                    module 0x1::M1 {}
                """
                )
                main(
                    """
                    module 0x1::M2 {}
                    script {
                        use 0x1::/*caret*/
                    }
                """
                )
            }
        }

    fun `test complete modules from all the files in fq path`() =
        checkContainsCompletionsExact(listOf("M1", "M2")) {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "module.move", """
                module 0x1::M1 {}                
            """
                )
                main(
                    """
                module 0x1::M2 {}
                script {
                    fun m() {
                        0x1::M/*caret*/
                    }
                }
            """
                )
            }
        }

    fun `test module completion with transitive dependency`() = doSingleCompletion(
        {
            moveToml(
                """
        [package]
        name = "Main"
        
        [dependencies]
        PontStdlib = { local = "./pont-stdlib" }                
            """
            )
            sources {
                move(
                    "main.move", """
            module 0x1::M {
                use Std::S/*caret*/
            }    
            """
                )
            }
            dir("pont-stdlib") {
                moveToml(
                    """ 
            [dependencies]
            MoveStdlib = { local = "./move-stdlib" }        
                """
                )
                dir("move-stdlib") {
                    moveToml(
                        """
                [addresses]
                Std = "0x1"
                    """
                    )
                    sources {
                        move(
                            "main.move", """
                        module Std::Signer {}    
                        """
                        )
                    }
                }
            }
        },
        """
        module 0x1::M {
            use Std::Signer/*caret*/
        }    
    """
    )

    fun `test named address is identified always with name even for the same address`() = checkNoCompletion {
        moveToml(
            """
        [package]
        name = "package"

        [addresses]
        Std = "0x1"
        Pontem = "0x1"
        """
        )
        sources {
            move(
                "mod.move", """
            module Std::StdMod {}
            module Pontem::PontemMod {}
            module 0x1::MyMod {
                use Std::Pont/*caret*/
            }    
            """
            )
        }
    }

    fun `test no named address completion in module position`() = checkNoCompletion {
        moveToml(
            """
        [package]
        name = "package"

        [addresses]
        Std = "0x1"
        Pontem = "0x2"
        """
        )
        sources {
            move(
                "mod.move", """
            module Std::StdMod {}
            module Pontem::MyMod {}
            module 0x1::MyMod {
                use 0x2::Pont/*caret*/
            }    
            """
            )
        }
    }

    fun `test no named address completion in item position`() = checkNoCompletion {
        moveToml(
            """
        [package]
        name = "package"

        [addresses]
        Std = "0x1"
        Pontem = "0x2"
        """
        )
        sources {
            move(
                "mod.move", """
            module Std::StdMod {}
            module Pontem::MyMod {}
            module 0x1::MyMod {
                use 0x2::MyMod::Pont/*caret*/
            }    
            """
            )
        }
    }

    fun `test completion for value address is available if identified with name`() = doSingleCompletion(
        {
            moveToml(
                """
        [package]
        name = "package"

        [addresses]
        Std = "0x1"
        Pontem = "0x2"
        """
            )
            sources {
                move(
                    "mod.move", """
            module Std::StdMod {}
            module Pontem::PontemMod {}
            module 0x1::MyMod {
                use 0x2::Pont/*caret*/
            }    
            """
                )
            }
        },
        """
            module Std::StdMod {}
            module Pontem::PontemMod {}
            module 0x1::MyMod {
                use 0x2::PontemMod/*caret*/
            }    
        """
    )

    fun `test modules from tests directory should not be in completion of sources`() = checkNoCompletion {
        namedMoveToml("MyPackage")
        sources {
            main(
                """
            module 0x1::Main {
                fun call() {
                    TestHe/*caret*/
                }
            }    
            """
            )
        }
        tests {
            move(
                "TestHelpers.move", """
            #[test_only]        
            module 0x1::TestHelpers {
            }    
            """
            )
        }
    }

    fun `test test_only modules from tests directory should not be in completion of sources`() =
        checkNoCompletion {
            namedMoveToml("MyPackage")
            sources {
                main(
                    """
            module 0x1::Main {
                fun call() {
                    TestHe/*caret*/
                }
            }    
            """
                )
            }
            tests {
                move(
                    "TestHelpers.move", """
            #[test_only]        
            module 0x1::TestHelpers {
            }    
            """
                )
            }
        }

    fun `test no completion if module address name is different`() = checkNoCompletion {
        moveToml(
            """
        [package]
        name = "MyPackage"
        [addresses]
        std = "0x1"
        endless_std = "0x1"
        """
        )
        sources {
            main(
                """
            module 0x1::M {
                use endless_stdlib::de/*caret*/;
            }    
            """
            )
            move(
                "debug.move", """
            module std::debug {
                public native fun print();
            }    
            """
            )
        }
    }
}
