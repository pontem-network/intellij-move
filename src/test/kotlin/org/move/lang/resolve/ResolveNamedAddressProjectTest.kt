package org.move.lang.resolve

import org.move.lang.core.psi.MoveNamedAddress
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
        refClass = MoveNamedAddress::class.java,
        targetClass = TomlKeySegment::class.java
    )
}
