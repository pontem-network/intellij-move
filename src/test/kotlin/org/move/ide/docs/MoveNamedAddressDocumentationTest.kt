package org.move.ide.docs

import org.move.utils.tests.MvDocumentationProviderProjectTestCase

class MvNamedAddressDocumentationTest : MvDocumentationProviderProjectTestCase() {
    fun `test value of named address accessible from documentation`() = doTestByFileTree(
        """
    //- Move.toml
    [addresses]
    Std = "0xCOFFEE"
    //- sources/main.move
    module Std::Module {}
          //^
        """, "Std = \"0xCOFFEE\"", block = MvDocumentationProvider::generateDoc
    )
}
