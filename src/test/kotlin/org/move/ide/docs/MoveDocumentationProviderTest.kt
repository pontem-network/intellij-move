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
    """, expected = "<div class='definition'><pre><b>move_from</b>(): R</pre></div>")

    private fun doTest(@Language("Rust") code: String, @Language("Html") expected: String?) =
        doTest(code, expected, block = MoveDocumentationProvider::generateDoc)
}
