package org.move.ide.hints

import org.move.lang.core.psi.MoveStructLiteralFieldsBlock
import org.move.utils.tests.ParameterInfoHandlerTestCase

class StructLiteralFieldsInfoHandlerTest :
    ParameterInfoHandlerTestCase<MoveStructLiteralFieldsBlock,
            FieldsDescription>(StructLiteralFieldsInfoHandler()) {

    fun `test no hint if call expr`() = checkByText("""
    module 0x1::M {
        struct S { s: u8 }
        fun call() {}
        fun m() {
            S { s: call(/*caret*/) };
        }
    }    
    """)

    fun `test hint if inside call expr`() = checkByText("""
    module 0x1::M {
        struct S {}
        fun call(s: S) {}
        fun m() {
            call(S { /*caret*/ });
        }
    }    
    """, "<no fields>", 0)

    fun `test show fields with position index 1`() = checkByText("""
    module M {
        struct S { a: u8, b: u8 }
        fun m() {
            S { a: 1, b: /*caret*/ };
        }
    }
    """, "a: u8, b: u8", 1)

    fun `test show -1 if all fields are filled`() = checkByText("""
    module M {
        struct S { a: u8, b: u8 }
        fun m() {
            S { a: 1, b: 2,/*caret*/ };
        }
    }
    """, "a: u8, b: u8", -1)

    fun `test show fields with position index 0 passed in reverse order`() = checkByText("""
    module M {
        struct S { a: u8, b: u8 }
        fun m() {
            S { b: 2, a: 1/*caret*/ };
        }
    }
    """, "a: u8, b: u8", 0)

    fun `test show next unfilled field in presence of filled`() = checkByText("""
    module M {
        struct S { a: u8, b: u8, c: u8, d: u8 }
        fun m() {
            S { a: 1, c: 2, /*caret*/ };
        }
    }
    """, "a: u8, b: u8, c: u8, d: u8", 1)

    fun `test highlight field caret in the middle`() = checkByText("""
    module 0x1::M {
        struct Collection { items: vector<u8>, items2: vector<u8> }
        fun m() {
            let myitems = b"123";
            Collection { items: myi/*caret*/tems, items2: myitems };
        }
    }    
    """, "items: vector<u8>, items2: vector<u8>", 0)

    fun `test highlight field caret at the end`() = checkByText("""
    module 0x1::M {
        struct Collection { items: vector<u8>, items2: vector<u8> }
        fun m() {
            let myitems = b"123";
            Collection { items: myitems/*caret*/, items2: myitems };
        }
    }    
    """, "items: vector<u8>, items2: vector<u8>", 0)
}
