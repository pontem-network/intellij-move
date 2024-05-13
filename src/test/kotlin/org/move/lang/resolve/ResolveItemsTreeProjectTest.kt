package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveProjectTestCase

class ResolveItemsTreeProjectTest : ResolveProjectTestCase() {

    fun `test resolve module from other file in sources folder`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move(
                "module.move", """
            module 0x1::Module {}
                      //X
            """
            )
            move(
                "main.move", """
            script {
                use 0x1::Module;
                       //^
            }    
            """
            )
        }
    }

    fun `test resolve module from file in local dependency`() = checkByFileTree {
        moveToml(
            """
        [dependencies]
        Stdlib = { local = "./stdlib" }
        """
        )
        sources {
            main(
                """
                script {
                    use 0x1::Module;
                           //^
                }
            """
            )
        }
        dir("stdlib") {
            namedMoveToml("Stdlib")
            sources {
                move(
                    "module.move", """
                    module 0x1::Module {}
                              //X
                """
                )
            }
        }
    }

    fun `test resolve module from other file with inline address`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move(
                "module.move", """
                module 0x1::Module {}
                          //X
            """
            )
            main(
                """
                script {
                    use 0x1::Module;
                           //^
                }
            """
            )
        }
    }

    fun `test resolve module from another file with named address`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "MyPackage"
        [addresses]
        Std = "0x1"    
        """
        )
        sources {
            move(
                "module.move", """
                module Std::Module {}
                          //X                
            """
            )
            main(
                """
                script {
                    use Std::Module;
                           //^
                }
            """
            )
        }
    }

    fun `test resolve module from dependency of dependency`() = checkByFileTree {
        moveToml(
            """
        [dependencies]
        PontStdlib = { local = "./pont-stdlib" }
        """
        )
        sources {
            move(
                "main.move", """
            module 0x1::M {
                use Std::Reflect;
                        //^
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
                    module Std::Reflect {}
                                //X
                """
                    )
                }
            }
        }
    }

    fun `test resolve namedaddr_module from dependency file with addr_subst`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "Main"    
        [dependencies]
        Stdlib = { local = "./stdlib", addr_subst = { Std = "0x1" } }
        """
        )
        sources {
            main(
                """
                script {
                    use Std::Module;
                           //^
                }        
        """
            )
        }
        dir("stdlib") {
            moveToml(
                """
            [package]
            name = "Stdlib"
            [addresses]
            Std = "_"                
            """
            )
            sources {
                move(
                    "module.move", """
                module Std::Module {}
                           //X                    
                """
                )
            }
        }
    }

    fun `test resolve module git dependency as inline table`() = checkByFileTree {
        dotMove {
            git("https://github.com/pontem-network/move-stdlib.git", "main") {
                moveToml(
                    """
                [package]
                name = "MoveStdlib"
                [addresses]
                Std = "0x1"    
                """
                )
                sources {
                    move(
                        "Vector.move", """
                    module Std::Vector {}
                              //X
                    """
                    )
                }
            }
        }
        moveToml(
            """
        [package]
        name = "MyModule"        
        [dependencies]
        MoveStdlib = { git = "https://github.com/pontem-network/move-stdlib.git", rev = "main" }
        """
        )
        sources {
            main(
                """
            script {
                use Std::Vector;
                       //^
                fun main() {}
            }    
            """
            )
        }
    }

    fun `test resolve module git dependency as table`() = checkByFileTree {
        dotMove {
            git("https://github.com/pontem-network/move-stdlib.git", "main") {
                moveToml(
                    """
                [package]
                name = "MoveStdlib"
                [addresses]
                Std = "0x1"    
                """
                )
                sources {
                    move(
                        "Vector.move", """
                    module Std::Vector {}
                              //X
                    """
                    )
                }
            }
        }
        moveToml(
            """
        [package]
        name = "MyModule"
        [dependencies.MoveStdlib]
        git = "https://github.com/pontem-network/move-stdlib.git"
        rev = "main"
        """
        )
        sources {
            move(
                "main.move", """
            script {
                use Std::Vector;
                       //^
                fun main() {}
            }    
            """
            )
        }
    }

    fun `test resolve module from git transitive dependency`() = checkByFileTree {
        dotMove {
            git("https://github.com/aptos-labs/pont-stdlib.git", "main") {
                moveToml(
                    """
        [package]
        name = "PontStdlib"        
        [dependencies]
        MoveStdlib = { git = "https://github.com/aptos-labs/move-stdlib.git", rev = "main" }                        
                """
                )
            }
            git("https://github.com/aptos-labs/move-stdlib.git", "main") {
                moveToml(
                    """
        [package]
        name = "MoveStdlib"        
                """
                )
                sources {
                    move(
                        "module.move", """
                    module Std::Module {}    
                                //X
                    """
                    )
                }
            }
        }
        moveToml(
            """
        [package]
        name = "MyModule"        
        [dependencies]
        PontStdlib = { git = "https://github.com/aptos-labs/pont-stdlib.git", rev = "main" }    
        """
        )
        sources {
            move(
                "main.move", """
            module Std::M {
                use Std::Module;
                        //^
            }    
            """
            )
        }
    }

    fun `test test_only items from tests are available in other tests`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources { }
        tests {
            move(
                "CoinTests.move", """
            #[test_only]    
            module 0x1::CoinTests {
                public fun call() {}
                          //X
            }    
            """
            )
            move(
                "TestCoinTests.move", """
            #[test_only]    
            module 0x1::TestCoinTests {
                use 0x1::CoinTests::call;
                
                fun main() {
                    call();
                    //^
                }
            }    
            """
            )
        }
    }

    fun `test resolve module by address value`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "MyPackage"
        [addresses]
        std = "0x1"
        aptos_std = "0x1"
        """
        )
        sources {
            main(
                """
            module 0x1::M {
                use aptos_std::debug;
                fun call() {
                    debug::print();
                   //^ 
                }
            }    
            """
            )
            move(
                "debug.move", """
            module std::debug {
                        //X
                public native fun print();
            }    
            """
            )
        }
    }

    fun `test resolve module from file in local dev-dependency`() = checkByFileTree {
        moveToml(
            """
        [dev-dependencies]
        Stdlib = { local = "./stdlib" }
        """
        )
        sources {
            main(
                """
                script {
                    use 0x1::Module;
                           //^
                }
            """
            )
        }
        dir("stdlib") {
            namedMoveToml("Stdlib")
            sources {
                move(
                    "module.move", """
                    module 0x1::Module {}
                              //X
                """
                )
            }
        }
    }

    fun `test resolve test only item inside test of nested package sources`() = checkByFileTree {
        namedMoveToml("RootPackage")
        tests {
            move(
                "test_helpers.move", """
            #[test_only]    
            module 0x1::test_helpers {
                public fun helper() {}
                          //X
                          
            }    
            """
            )
        }
        dir("NestedPackage") {
            moveToml(
                """
            [package]
            name = "NestedPackage"
            
            [dependencies]
            RootPackage = { local = ".." }
            """
            )
            sources {
                main(
                    """
                #[test_only]    
                module 0x1::main {
                    use 0x1::test_helpers;
                    
                    #[test]
                    fun test_end_to_end() {
                        test_helpers::helper();
                                     //^
                    }
                }                    
                """
                )
            }
        }
    }

    fun `test resolve test only item inside test of nested package test`() = checkByFileTree {
        namedMoveToml("RootPackage")
        tests {
            move(
                "test_helpers.move", """
            #[test_only]    
            module 0x1::test_helpers {
                public fun helper() {}
                          //X
                          
            }    
            """
            )
        }
        dir("NestedPackage") {
            moveToml(
                """
            [package]
            name = "NestedPackage"
            
            [dependencies]
            RootPackage = { local = ".." }
            """
            )
            tests {
                main(
                    """
                #[test_only]    
                module 0x1::main {
                    use 0x1::test_helpers;
                    
                    #[test]
                    fun test_end_to_end() {
                        test_helpers::helper();
                                     //^
                    }
                }                    
                """
                )
            }
        }
    }

//    fun `test expected failure location`() = checkByFileTree {
//        moveToml("""
//        [package]
//        name = "Root"
//
//        [addresses]
//        aptos_framework = "0x1"
//        """)
//        sources {
//            move("account.move", """
//                module aptos_framework::account {}
//                                          //X
//            """)
//            move("account_tests.move", """
//                module aptos_framework::account_tests {
//                    #[test]
//                    #[expected_failure(abort_code = 1, location = aptos_framework::account)]
//                                                                                  //^
//                    fun test_abort() {
//
//                    }
//                }
//            """)
//        }
//    }

    fun `test cannot resolve module ref that belongs to another project`() = checkByFileTree {
        dir("another") {
            namedMoveToml("Another")
            sources {
                move("string.move", """
module 0x1::string {}                    
                """)
            }
        }
        namedMoveToml("Main")
        sources {
            main("""
module 0x1::main {
    use 0x1::string;
            //^ unresolved
}                
            """)
        }
    }

    fun `test resolve vector method if stdlib vector module present`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move("vector.move", """
        module 0x1::vector {
            public native fun length<T>(self: &vector<T>): u8;
                               //X
        }        
            """)
            main("""
        module 0x1::main {
            fun main() {
                vector[1].length();
                          //^ 
            }
        }
            """)
        }
    }

    fun `test resolve vector reference method if stdlib vector module present`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move("vector.move", """
        module 0x1::vector {
            public native fun length<T>(self: &vector<T>): u8;
                               //X
        }        
            """)
            main("""
        module 0x1::main {
            fun main() {
                (&vector[1]).length();
                             //^ 
            }
        }
            """)
        }
    }

    fun `test vector method unresolved if no stdlib module`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            main("""
        module 0x1::main {
            fun main() {
                vector[1].length();
                          //^ unresolved
            }
        }
            """)
        }
    }

    fun `test vector method unresolved if address of vector module is different`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move("vector.move", """
        module 0x2::vector {
            public native fun length<T>(self: &vector<T>): u8;
        }        
            """)
            main("""
        module 0x1::main {
            fun main() {
                vector[1].length();
                          //^ unresolved
            }
        }
            """)
        }
    }
}
