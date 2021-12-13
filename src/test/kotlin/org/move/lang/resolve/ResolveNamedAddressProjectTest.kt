package org.move.lang.resolve

import org.move.lang.core.psi.MvNamedAddress
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.resolve.ResolveProjectTestCase
import org.toml.lang.psi.TomlKeySegment

class ResolveNamedAddressProjectTest : ResolveProjectTestCase() {
    fun `test resolve named address to toml key`() = checkByFileTree {
        moveToml("""
        [addresses]
        Std = "0x1"
        #X    
        """)
        sources {
            move("main.move", """
            module Std::Module {}
                  //^
            """)
        }
    }

    fun `test resolve named address to toml key of the dependency`() = checkByFileTree {
        moveToml("""
        [dependencies]
        Stdlib = { local = "./stdlib", addr_subst = { "Std" = "0x1" } }    
        """)
        dir("stdlib") {
            moveToml("""
            [addresses]
            Std = "_"
            #X    
            """)
        }
        sources {
            move("main.move", """
            module Std::Module {}
                 //^     
            """)
        }
    }

    fun `test named address to renamed dep address`() = checkByFileTree {
        moveToml("""
        [dependencies]
        Stdlib = { local = "./stdlib", addr_subst = { "StdX" = "Std" } }
                                                       #X
        """)
        dir("stdlib") {
            moveToml("""
            [addresses]
            Std = "0x1"
            """)
        }
        sources {
            move("main.move", """
            module StdX::Module {}
                 //^     
            """)
        }
    }

//    fun `test renamed dep address to dep declaration`() = checkByFileTree {
//        moveToml("""
//        [dependencies]
//        Stdlib = { local = "./stdlib", addr_subst = { "StdX" = "Std" } }
//                                                                #^
//        """)
//        dir("stdlib") {
//            moveToml("""
//            [addresses]
//            Std = "0x1"
//            #X
//            """)
//        }
//    }

    override fun checkByFileTree(fileTree: FileTreeBuilder.() -> Unit) {
        checkByFileTree(MvNamedAddress::class.java,
                        TomlKeySegment::class.java,
                        fileTree)
    }
}
