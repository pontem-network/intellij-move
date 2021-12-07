package org.move.lang.resolve

import org.move.lang.core.psi.MvNamedAddress
import org.move.utils.tests.resolve.ResolveProjectTestCase
import org.toml.lang.psi.TomlKeySegment

class ResolveNamedAddressProjectTest : ResolveProjectTestCase() {
    fun `test resolve named address to toml key`() = checkByFileTree(
        """
        //- Move.toml
        [addresses]
        Std = "0x1"
        #X 
        //- sources/main.move
        module Std::Module {}
             //^ 
        """,
        refClass = MvNamedAddress::class.java,
        targetClass = TomlKeySegment::class.java
    )

    fun `test resolve named address to toml key of the dependency`() = checkByFileTree(
        """
        //- Move.toml
        [dependencies]
        Stdlib = { local = "./stdlib" }
        //- stdlib/Move.toml
        [addresses]
        Std = "0x1"
        #X 
        //- sources/main.move
        module Std::Module {}
             //^ 
        """,
        refClass = MvNamedAddress::class.java,
        targetClass = TomlKeySegment::class.java
    )
}
