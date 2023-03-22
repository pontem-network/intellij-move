package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class RequiredParensForCastExprInspectionTest : InspectionTestBase(RequiredParensForCastExprInspection::class) {
    fun `test cast expr in parens`() = checkByText("""
module 0x1::main {
    fun main() {
        let a = 1;
        (a as u64);
    }
}        
    """)

    fun `test error if cast expr without parens`() = checkByText("""
module 0x1::main {
    fun main() {
        let a = 1;
        a <error descr="Parentheses are required for the cast expr">as u64</error>;
    }
}        
    """)

    fun `test error if complex cast expr without parens`() = checkByText("""
module 0x1::main {
    fun main() {
        let a = 1;
        (a.b.c + 1) <error descr="Parentheses are required for the cast expr">as u64</error>;
    }
}        
    """)

    fun `test cast inside other expr`() = checkByText("""
module 0x1::main {
    fun main() {
        let a = 1;
        1 + a <error descr="Parentheses are required for the cast expr">as u64</error>; 
    }
}                
    """)
}
