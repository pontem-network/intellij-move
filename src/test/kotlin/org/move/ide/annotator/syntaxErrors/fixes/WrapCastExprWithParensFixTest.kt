package org.move.ide.annotator.syntaxErrors.fixes

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class WrapCastExprWithParensFixTest : AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    fun `test error if cast expr without parens`() = checkFixByText(
        "Wrap cast with ()",
        """
module 0x1::main {
    fun main() {
        let a = 1;
        <error descr="Parentheses are required for the cast expr">a /*caret*/as u64</error>;
    }
}        
    """,
        """
module 0x1::main {
    fun main() {
        let a = 1;
        (a as u64);
    }
}        
    """,
    )

    fun `test wrap cast of binary expr`() = checkFixByText(
        "Wrap cast with ()",
        """
module 0x1::main {
    fun main() {
        let a = 1;
        <error descr="Parentheses are required for the cast expr">1 + 1 + a /*caret*/as u64</error>; 
    }
}        
    """,
        """
module 0x1::main {
    fun main() {
        let a = 1;
        (1 + 1 + a as u64); 
    }
}        
    """,
    )
}
