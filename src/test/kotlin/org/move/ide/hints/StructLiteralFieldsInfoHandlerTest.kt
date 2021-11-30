package org.move.ide.hints

import org.move.lang.core.psi.MoveStructLiteralFieldsBlock
import org.move.utils.tests.ParameterInfoHandlerTestCase

class StructLiteralFieldsInfoHandlerTest :
    ParameterInfoHandlerTestCase<MoveStructLiteralFieldsBlock,
            StructLiteralFieldsDescription>(StructLiteralFieldsInfoHandler()) {

    fun `test no fields`() = checkByText("""
    module M {
        struct S {}
        fun m() {
            S { /*caret*/ };
        }
    }
    """, "<no fields>", 0)

    fun `test show fields with position`() = checkByText("""
    module M {
        struct S { a: u8, b: u8 }
        fun m() {
            S { a: 1, b: /*caret*/ };
        }
    }
    """, "a: u8, b: u8", 1)
}
