package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveProjectTestCase

class ResolveModulesProjectTest : ResolveProjectTestCase() {

    fun `test resolve module from other file in sources folder`() = checkByFileTree {
        moveToml()
        sources {
            move("module.move", """
            module 0x1::Module {}
                      //X
            """)
            move("main.move", """
            script {
                use 0x1::Module;
                       //^
            }    
            """)
        }
    }

    fun `test resolve module from file in local dependency`() = checkByFileTree(
        """
        //- Move.toml
        [dependencies]
        Stdlib = { local = "./stdlib" }
        
        //- stdlib/Move.toml
        //- stdlib/sources/module.move
        module 0x1::Module {}
                  //X
        //- sources/main.move
        script {
            use 0x1::Module;
                   //^
        }    
    """
    )

    fun `test resolve module from other file with inline address`() = checkByFileTree(
        """
        //- Move.toml
        //- sources/module.move
        module 0x1::Module {}
                  //X
        //- sources/main.move
        script {
            use 0x1::Module;
                   //^
        }
    """
    )

    fun `test resolve module from another file with named address`() = checkByFileTree("""
        //- Move.toml
        [addresses]
        Std = "0x1"
        //- sources/module.move
        module Std::Module {}
                  //X
        //- sources/main.move
        script {
            use Std::Module;
                   //^
        }
    """)

    fun `test resolve namedaddr_module from dependency file with addr_subst`() = checkByFileTree("""
        //- Move.toml
        [dependencies]
        Stdlib = { local = "./stdlib", addr_subst = { Std = "0x1" } }
        
        //- stdlib/Move.toml
        [addresses]
        Std = "_"
        //- stdlib/sources/module.move
        module Std::Module {}
                  //X
        //- sources/main.move
        script {
            use Std::Module;
                   //^
        }    
    """)

    fun `test resolve module to one inside build directory if git dependency and present`() = checkByFileTree {
        build {
            dir("MoveStdlib") {
                sources {
                    move("Vector.move", """
                    module Std::Vector {}
                              //X
                    """)
                }
                buildInfo("""
                ---
                compiled_package_info: 
                  package_name: MoveStdlib
                  address_alias_instantiation:
                    Std: "0000000000000000000000000000000000000000000000000000000000000001"
                  module_resolution_metadata:
                    ? address: "0000000000000000000000000000000000000000000000000000000000000001"
                      name: Vector
                    : Std
                  source_digest: AF352E5D4EBF95696119D21119B997C20F6F988E791CC058B7C7D4FAFA2B7CEF
                dependencies: []    
                """)
            }
        }
        moveToml("""
        [dependencies]
        MoveStdlib = { git = "git@github.com:pontem-network/move-stdlib.git", rev = "fdeb555c2157a1d68ca64eaf2a2e2cfe2a64efa2" }
        """)
        sources {
            move("main.move", """
            script {
                use Std::Vector;
                       //^
                fun main() {}
            }    
            """)
        }
    }
}
