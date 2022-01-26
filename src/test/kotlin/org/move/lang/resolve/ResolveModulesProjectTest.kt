package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveProjectTestCase

class ResolveModulesProjectTest : ResolveProjectTestCase() {

    fun `test resolve module from other file in sources folder`() = checkByFileTree {
        moveToml()
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

    fun `test resolve module from another file with named address`() = checkByFileTree(
        """
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
    """
    )

    fun `test resolve module from dependency of dependency`() = checkByFileTree {
        moveToml(
        """
        [dependencies]
        PontStdlib = { local = "./pont-stdlib" }
        """
        )
        sources {
            move("main.move", """
            module 0x1::M {
                use Std::Reflect;
                        //^
            }     
            """)
        }
        dir("pont-stdlib", {
            moveToml("""
            [dependencies]
            MoveStdlib = { local = "./move-stdlib" }    
            """)
            dir("move-stdlib", {
                moveToml("""
                [addresses]
                Std = "0x1"    
                """)
                sources { move("main.move", """
                    module Std::Reflect {}
                                //X
                """) }
            })
        })
    }


    fun `test resolve namedaddr_module from dependency file with addr_subst`() = checkByFileTree(
        """
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
    """
    )

    fun `test resolve module git dependency as inline table`() = checkByFileTree {
        build {
            dir("MoveStdlib") {
                sources {
                    move(
                        "Vector.move", """
                    module Std::Vector {}
                              //X
                    """
                    )
                }
                buildInfoYaml("""
---
compiled_package_info:
  package_name: Stdlib
  address_alias_instantiation:
    Std: "0000000000000000000000000000000000000000000000000000000000000002"
dependencies: []
                """)
            }
        }
        moveToml(
            """
        [dependencies]
        MoveStdlib = { git = "git@github.com:pontem-network/move-stdlib.git", rev = "fdeb555c2157a1d68ca64eaf2a2e2cfe2a64efa2" }
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

    fun `test resolve module git dependency as table`() = checkByFileTree {
        build {
            dir("MoveStdlib") {
                sources {
                    move(
                        "Vector.move", """
                    module Std::Vector {}
                              //X
                    """
                    )
                }
                buildInfoYaml("""
---
compiled_package_info:
  package_name: Stdlib
  address_alias_instantiation:
    Std: "0000000000000000000000000000000000000000000000000000000000000002"
dependencies: []
                """)
            }
        }
        moveToml(
            """
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
        build {
            dir("MoveStdlib") {
                buildInfoYaml("""
---
compiled_package_info:
  package_name: MoveStdlib
  address_alias_instantiation:
    Std: "0000000000000000000000000000000000000000000000000000000000000002"
  module_resolution_metadata:
  source_digest: 2C309D3225F22451CD0BCB9C2D6655FB3CADCEB5091983160E9FF3573BBF7797
  build_flags:
    dev_mode: false
    test_mode: false
    generate_docs: false
    generate_abis: false
    install_dir: ~
    force_recompilation: false
    additional_named_addresses: {}
dependencies: []
                """)
                sources {
                    move("module.move", """
                    module Std::Module {}    
                                //X
                    """)
                }
            }
            dir("PontStdlib") {
                buildInfoYaml("""
---
compiled_package_info:
  package_name: PontStdlib
  address_alias_instantiation:
    Std: "0000000000000000000000000000000000000000000000000000000000000002"  
  module_resolution_metadata:
  source_digest: 2C309D3225F22451CD0BCB9C2D6655FB3CADCEB5091983160E9FF3573BBF7797
  build_flags:
    dev_mode: false
    test_mode: false
    generate_docs: false
    generate_abis: false
    install_dir: ~
    force_recompilation: false
    additional_named_addresses: {}
dependencies: 
  - MoveStdlib
                """)
            }
        }
        moveToml("""
        [dependencies]
        PontStdlib = { git = "", rev = "" }    
        """)
        sources {
            move("main.move", """
            module Std::M {
                use Std::Module;
                        //^
            }    
            """)
        }
    }
}
