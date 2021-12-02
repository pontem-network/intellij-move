package org.move.ide.docs

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveDocumentationProviderTestCase

class MoveDocumentationProviderTest : MoveDocumentationProviderTestCase() {
    fun `test show docs for move_from`() = doTest("""
    module M {
        fun m() {
            move_from();
          //^  
        }
    }
    """, expected = """
        <div class='definition'><pre>builtin_functions
        native fun <b>move_from</b>&lt;R: key&gt;(addr: address): R</pre></div>
        <div class='content'></div>
        """)

    fun `test show doc comment for module`() = doTest("""
    /// module docstring
    module 0x1::M {}
              //^   
    """, expected = """
        <div class='definition'><pre>module 0x1::M</pre></div>
        <div class='content'><p>module docstring</p></div>
        """
    )

    fun `test show doc comment for const`() = doTest("""
    module 0x1::M {
        /// const docstring
        const ERR_COLLECTION_IS_ALREADY_EXISTS: u64 = 1;
            //^
    }    
    """, expected = """
        <div class='definition'><pre>0x1::M
        const <b>ERR_COLLECTION_IS_ALREADY_EXISTS</b>: u64 = 1</pre></div>
        <div class='content'><p>const docstring</p></div>    
    """)

    fun `test show doc comments and signature for function`() = doTest("""
    module 0x1::M {
        /// Adds two numbers.
        /// Returns their sum.
        fun add(a: u8, b: u8): u8 {
          //^
          a + b
        }
    }
    """, expected = """
        <div class='definition'><pre>0x1::M
        fun <b>add</b>(a: u8, b: u8): u8</pre></div>
        <div class='content'><p>Adds two numbers.</p>
        <p>Returns their sum.</p></div>
    """)

    fun `test show signature for function parameter`() = doTest("""
    module 0x1::M {
        fun add(a: u8, b: u8): u8 {
          a
        //^  
        }
    }
    """, expected = """
        value parameter <b>a</b>: u8
    """)

    fun `test show signature for type parameter`() = doTest("""
    module 0x1::M {
        fun move_r<R: store + drop>(res: R): R {
                                       //^
        }
    }
    """, expected = """
        type parameter <b>R</b>: store + drop
    """)

    fun `test show signature for simple let variable`() = doTest("""
    module 0x1::M {
        fun m() {
          let a: vector<u8>;
          a;
        //^ 
        }
    }
    """, expected = """
        variable <b>a</b>: vector&lt;u8&gt;
    """)

    fun `test struct docstring`() = doTest("""
    module 0x1::M {
        /// docstring
        struct S<R: store> has copy, drop, store {}
        fun m() {
            S { };
          //^  
        }
    }    
    """, expected = """
        <div class='definition'><pre>0x1::M
        struct <b>S</b>&lt;R: store&gt; has copy, drop, store</pre></div>
        <div class='content'><p>docstring</p></div>
    """)

    fun `test struct field as vector`() = doTest(
        """
    module 0x1::M {
        struct NFT {}
        struct Collection has key {
            /// docstring
            nfts: vector<NFT> 
        }
        fun m() acquires Collection {
            let coll = borrow_global_mut<Collection>(@0x1);
            coll.nfts;
               //^
        }
    }    
    """, expected = """
        <div class='definition'><pre>0x1::M::Collection
        <b>nfts</b>: vector&lt;0x1::M::NFT&gt;</pre></div>
        <div class='content'><p>docstring</p></div>"""
    )

    private fun doTest(@Language("Move") code: String, @Language("Html") expected: String?) =
        doTest(code, expected, block = MoveDocumentationProvider::generateDoc)
}
