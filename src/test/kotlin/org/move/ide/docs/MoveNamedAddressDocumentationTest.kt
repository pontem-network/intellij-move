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
}
