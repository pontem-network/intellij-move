package org.move.ide.typing

import org.move.utils.tests.MvTypingTestCase

class MvAngleBraceTypedHandlerTest : MvTypingTestCase() {
    fun `test add second pair of angle brackets`() = doTest(
        "script { fun main<NFT/*caret*/>() {}}",
        '<',
        "script { fun main<NFT</*caret*/>>() {}}",
    )

    fun `test dont pair braces inside identifier`() = doTest(
        "script { fun main<NF/*caret*/T>() {}}",
        '<',
        "script { fun main<NF</*caret*/T>() {}}",
    )

    fun `test don't pair angle brace if braces aren't balanced`() = doTest(
        "script { fun main/*caret*/>() {}}",
        '<',
        "script { fun main</*caret*/>() {}}",
    )

    fun `test add pair of angle brackets for struct field`() = doTest(
        "module 0x1::M { struct Col { a: Option/*caret*/ } }",
        '<',
        "module 0x1::M { struct Col { a: Option</*caret*/> } }",
    )

    fun `test closing brace just moves the caret`() =
        doTest(
            "module 0x1::M { fun m() { Res<E, X/*caret*/> } }",
            '>',
            "module 0x1::M { fun m() { Res<E, X>/*caret*/ } }"
        )

    fun `test right angle is inserted if it is not a closing brace`() =
        doTest(
            "module 0x1::M { fun m() { Res<E, X>/*caret*/; } }",
            '>',
            "module 0x1::M { fun m() { Res<E, X>>/*caret*/; } }"
        )

    fun `test don't remove next GT if braces aren't balanced`() =
        doTest(
            "module 0x1::M { fun m() { Res<</*caret*/>; } }",
            '\b',
            "module 0x1::M { fun m() { Res</*caret*/>; } }"
        )

}
