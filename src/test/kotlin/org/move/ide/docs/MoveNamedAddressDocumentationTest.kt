package org.move.ide.docs

import org.move.utils.tests.MoveDocumentationProviderProjectTestCase

class MoveNamedAddressDocumentationTest : MoveDocumentationProviderProjectTestCase() {
//    fun `test value of named address accessible from documentation`() = doTestByFileTree(
//        """
//    //- Move.toml
//    [addresses]
//    Std = "0xCOFFEE"
//    //- sources/main.move
//    module Std::Module {}
//         //^
//        """, "0xCOFFEE", block = MoveDocumentationProvider::generateDoc
//    )

    // TODO: check parsing in different test
    fun `test value of named address for imported and substituted`() = doTestByFileTree(
        """
    //- Move.toml
    [package]
    name = "rmrk"
    version = "0.0.0"
    
    [dependencies]
    Stdlib = { local = "./stdlib", addr_subst = { "Std" = "0xC0FFEE" }}
    //- stdlib/Move.toml
    [package]
    name = "Stdlib"
    version = "0.0.0"
    [addresses]
    Std = "_"
    //- stdlib/sources/module.move
    //- sources/main.move
    module Std::Module {}       
         //^   
        """, "0xC0FFEE", block = MoveDocumentationProvider::generateDoc
    )
}
