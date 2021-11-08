package org.move.ide.docs

import org.move.utils.tests.MoveDocumentationProviderProjectTestCase

class MoveNamedAddressDocumentationTest : MoveDocumentationProviderProjectTestCase() {
    fun `test value of named address accessible from documentation`() = doTestByFileTree(
        """
    //- Move.toml
    [addresses]
    Std = "0xCOFFEE"
    //- sources/main.move
    module Std::Module {}        
         //^   
        """, "0xCOFFEE", block = MoveDocumentationProvider::generateDoc
    )

    fun `test value of named address for imported and substituted`() = doTestByFileTree(
        """
    //- Move.toml
    [dependencies]
    Stdlib = { local = "./stdlib", addr_subst = { "Std" = "0xC0FFEE" }}
    //- stdlib/Move.toml
    [addresses]
    Std = "_"
    //- sources/main.move
    module Std::Module {}        
         //^   
        """, "0xC0FFEE", block = MoveDocumentationProvider::generateDoc
    )
}
