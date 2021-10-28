package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveProjectTestCase

class ResolveMoveProjectModulesTest : ResolveProjectTestCase() {
    fun `test resolve module from other file in sources folder`() = checkByFileTree(
        """
        //- Move.toml
        //- sources/module.move
        address 0x1 {
            module Module {}
                 //X
        }    
        //- sources/main.move
        script {
            use 0x1::Module;
                   //^
        }    
    """
    )

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
}
