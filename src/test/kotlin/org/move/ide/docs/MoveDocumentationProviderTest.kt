package org.move.ide.docs

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveDocumentationProviderTestCase

class MoveDocumentationProviderTest : MoveDocumentationProviderTestCase() {
    fun `test variable`() = doTest(
        """
    module M {
        fun main() {
            let a = 1u64;
            a;
          //^  
        }
    }    
    """, "u64"
    )

    fun `test show docs for move_from`() = doTest("""
    module M {
        fun m() {
            move_from();
          //^  
        }
    }
    """, expected = """
        <div class='definition'><pre><b>move_from</b>(addr: address): R</pre></div>
        <div class='content'></div>
        """)

    fun `test show doc comments and signature for function`() = doTest("""
    module M {
        /// Adds two numbers.
        /// Returns their sum.
        fun add(a: u8, b: u8): u8 {
          //^
          a + b
        }
    }
    """, expected = """
        <div class='definition'><pre><b>add</b>(a: u8, b: u8): u8</pre></div>
        <div class='content'><p>Adds two numbers.</p>
        <p>Returns their sum.</p></div>
    """)

    fun `test struct field as vector`() = doTest("""
    module 0x1::M {
        struct NFT {}
        struct Collection { nfts: vector<NFT> }
        fun m() {
            let coll = borrow_global_mut<Collection>(0x1);
            coll.nfts
               //^
        }
    }    
    """, expected = "vector<0x1::M::NFT>")

    private fun doTest(@Language("Move") code: String, @Language("Html") expected: String?) =
        doTest(code, expected, block = MoveDocumentationProvider::generateDoc)
}
