package org.move.ide.inspections.fixes

import org.move.ide.inspections.RequiredParensForCastExprInspection
import org.move.utils.tests.annotation.InspectionTestBase

class WrapWithParensExprFixTest : InspectionTestBase(RequiredParensForCastExprInspection::class) {
    fun `test error if cast expr without parens`() = checkFixByText(
        "Add parentheses to `a as u64`",
        """
module 0x1::main {
    fun main() {
        let a = 1;
        a <error descr="Parentheses are required for the cast expr">/*caret*/as u64</error>;
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
        "Add parentheses to `1 + 1 + a as u64`",
        """
module 0x1::main {
    fun main() {
        let a = 1;
        1 + 1 + a <error descr="Parentheses are required for the cast expr">/*caret*/as u64</error>; 
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

    fun `test wrap cast of atom expr`() = checkFixByText(
        "Add parentheses to `a as u64`",
        """
module 0x1::main {
    fun main() {
        let a = 1;
        1 + 1 + a <error descr="Parentheses are required for the cast expr">/*caret*/as u64</error>; 
    }
}        
    """,
        """
module 0x1::main {
    fun main() {
        let a = 1;
        1 + 1 + (a as u64); 
    }
}        
    """,
    )
}
