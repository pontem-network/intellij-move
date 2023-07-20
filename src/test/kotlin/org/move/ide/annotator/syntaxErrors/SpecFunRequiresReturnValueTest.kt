package org.move.ide.annotator.syntaxErrors

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class SpecFunRequiresReturnValueTest : AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    fun `test spec fun with explicit return value`() = checkWarnings(
        """
        module 0x1::m {
            spec fun value(): u8 { 1 }
            spec fun vec(): vector<u8> { vector[1] }
        }
    """
    )

    fun `test spec fun requires return value`() = checkWarnings(
        """
        module 0x1::m {
            spec fun value()<error descr="Requires return type"> {</error>}
            spec fun vec()<error descr="Requires return type"> {</error>}
        }
    """
    )
}
