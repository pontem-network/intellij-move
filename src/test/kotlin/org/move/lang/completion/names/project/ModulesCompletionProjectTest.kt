package org.move.lang.completion.names.project

import org.move.utils.tests.completion.CompletionProjectTestCase

class ModulesCompletionProjectTest : CompletionProjectTestCase() {

    fun `test complete modules from all the files in imports`() = checkContainsCompletionsExact(
        """
        //- Move.toml
        //- sources/module.move
        module 0x1::M1 {}
        //- sources/main.move
        module 0x1::M2 {}
        script {
            use 0x1::/*caret*/
        }
    """, listOf("M1", "M2")
    )

    fun `test complete modules from all the files in fq path`() = checkContainsCompletionsExact(
        """
        //- Move.toml
        //- sources/module.move
        module 0x1::M1 {}
        //- sources/main.move
        module 0x1::M2 {}
        script {
            fun m() {
                0x1::M/*caret*/
            }
        }
    """, listOf("M1", "M2")
    )

    fun `test module completion with transitive dependency`() = doSingleCompletion(
        {
            moveToml("""
        [package]
        name = "Main"
        
        [dependencies]
        PontStdlib = { local = "./pont-stdlib" }                
            """)
            sources {
                move("main.move", """
            module 0x1::M {
                use Std::S/*caret*/
            }    
            """)
            }
            dir("pont-stdlib") {
                moveToml(""" 
            [dependencies]
            MoveStdlib = { local = "./move-stdlib" }        
                """)
                dir("move-stdlib") {
                    moveToml("""
                [addresses]
                Std = "0x1"
                    """)
                    sources {
                        move("main.move", """
                        module Std::Signer {}    
                        """)
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

    fun `test named address is identified always with name even for same address`() = checkNoCompletion {
        moveToml("""
        [package]
        name = "package"

        [addresses]
        Std = "0x1"
        Pontem = "0x1"
        """)
        sources {
            move("mod.move", """
            module Std::StdMod {}
            module Pontem::PontemMod {}
            module 0x1::MyMod {
                use Std::Pont/*caret*/
            }    
            """)
        }
    }

    fun `test named address is identified only with name`() = checkNoCompletion {
        moveToml("""
        [package]
        name = "package"

        [addresses]
        Std = "0x1"
        Pontem = "0x1"
        """)
        sources {
            move("mod.move", """
            module Std::StdMod {}
            module Pontem::PontemMod {}
            module 0x1::MyMod {
                use 0x1::Pont/*caret*/
            }    
            """)
        }
    }
}
