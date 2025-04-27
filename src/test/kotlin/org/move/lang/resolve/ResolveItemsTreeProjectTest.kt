package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveProjectTestCase

class ResolveItemsTreeProjectTest: ResolveProjectTestCase() {

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

    fun `test resolve module by address value with different address name`() = checkByFileTree {
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
                move(
                    "string.move", """
module 0x1::string {}                    
                """
                )
            }
        }
        namedMoveToml("Main")
        sources {
            main(
                """
module 0x1::main {
    use 0x1::string;
            //^ unresolved
}                
            """
            )
        }
    }

    fun `test resolve module that has the same name as address`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "Main"
        
        [dependencies]
        UQ64x64 = { local = "./uq64x64" }
            """
        )
        sources {
            main(
                """
        module 0x1::m {
            use uq64x64::uq64x64;
                        //^            
        }
            """
            )
        }
        dir("uq64x64") {
            moveToml(
                """
        [package]
        name = "UQ64x64"
        
        [addresses]
        uq64x64 = "0x4e9fce03284c0ce0b86c88dd5a46f050cad2f4f33c4cdd29d98f501868558c81"
            """
            )
            sources {
                move(
                    "uq64x64.move", """
            module uq64x64::uq64x64 {
                            //X
            }
                """
                )
            }
        }
    }

    fun `test resolve module in path from module that has the same name as address`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "Main"
        
        [dependencies]
        UQ64x64 = { local = "./uq64x64" }
            """
        )
        sources {
            main(
                """
        module 0x1::m {
            use uq64x64::uq64x64;
            fun main() {
                uq64x64::call();
                 //^
            }
        }
            """
            )
        }
        dir("uq64x64") {
            moveToml(
                """
        [package]
        name = "UQ64x64"
        
        [addresses]
        uq64x64 = "0x4e9fce03284c0ce0b86c88dd5a46f050cad2f4f33c4cdd29d98f501868558c81"
            """
            )
            sources {
                move(
                    "uq64x64.move", """
            module uq64x64::uq64x64 {
                           //X
                public fun call() {}
            }
                """
                )
            }
        }
    }

    fun `test resolve function from module that has the same name as address`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "Main"
        
        [dependencies]
        UQ64x64 = { local = "./uq64x64" }
            """
        )
        sources {
            main(
                """
        module 0x1::m {
            use uq64x64::uq64x64;
            fun main() {
                uq64x64::call();
                        //^     
            }
        }
            """
            )
        }
        dir("uq64x64") {
            moveToml(
                """
        [package]
        name = "UQ64x64"
        
        [addresses]
        uq64x64 = "0x4e9fce03284c0ce0b86c88dd5a46f050cad2f4f33c4cdd29d98f501868558c81"
            """
            )
            sources {
                move(
                    "uq64x64.move", """
            module uq64x64::uq64x64 {
                public fun call() {}
                           //X
            }
                """
                )
            }
        }
    }

    fun `test resolve item from module with dev only address`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "Main"
        
        [dev-addresses]
        dev = "0x3"
            """
        )
        sources {
            main(
                """
        module 0x1::m {
            use dev::dev_module::call;
            fun main() {
                call();
               //^     
            }
        }
            """
            )
            move(
                "dev_module.move", """
        module dev::dev_module {
            public fun call() {}
                      //X
        }
            """
            )
        }
    }

    fun `test resolve item from module with dev address with placeholder`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "Main"
        
        [addresses]
        dev = "_"
        
        [dev-addresses]
        dev = "0x3"
            """
        )
        sources {
            main(
                """
        module 0x1::m {
            use dev::dev_module::call;
            fun main() {
                call();
               //^     
            }
        }
            """
            )
            move(
                "dev_module.move", """
        module dev::dev_module {
            public fun call() {}
                      //X
        }
            """
            )
        }
    }

    fun `test resolve item from dependency with dev only address`() = checkByFileTree {
        moveToml(
            """
        [package]
        name = "Main"
        
        [dependencies]
        Local = { local = "./dev_dep" }
            """
        )
        sources {
            main(
                """
        module 0x1::m {
            use dev::dev_module::call;
            fun main() {
                call();
               //^     
            }
        }
            """
            )
        }
        dir("dev_dep") {
            moveToml(
                """
        [package]
        name = "DevDep"
        
        [dev-addresses]
        dev = "0x3"
            """
            )
            sources {
                move(
                    "dev_module.move", """
        module dev::dev_module {
            public fun call() {}
                      //X
        }
            """
                )
            }
        }
    }
}
