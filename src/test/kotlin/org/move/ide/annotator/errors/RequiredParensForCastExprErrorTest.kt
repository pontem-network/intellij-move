package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class RequiredParensForCastExprErrorTest : AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test cast expr in parens`() = checkErrors(
        """
module 0x1::main {
    fun main() {
        let a = 1;
        (a as u64);
    }
}        
    """
    )

    fun `test error if cast expr without parens`() = checkErrors(
        """
module 0x1::main {
    fun main() {
        let a = 1;
        <error descr="Parentheses are required for the cast expr">a as u64</error>;
    }
}        
    """
    )

    fun `test error if complex cast expr without parens`() = checkErrors(
        """
module 0x1::main {
    fun main() {
        let a = 1;
        <error descr="Parentheses are required for the cast expr">(a.b.c + 1) as u64</error>;
    }
}        
    """
    )

    fun `test cast inside other expr`() = checkErrors(
        """
module 0x1::main {
    fun main() {
        let a = 1;
        <error descr="Parentheses are required for the cast expr">1 + a as u64</error>; 
    }
}                
    """
    )
}
