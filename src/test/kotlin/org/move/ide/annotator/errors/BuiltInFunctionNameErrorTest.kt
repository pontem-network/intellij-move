package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class BuiltInFunctionNameErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test function`() = checkErrors("""
        module 0x1::M {
            fun <error descr="Invalid function name: `assert` is a built-in function">assert</error>() {}
        }
    """)

    fun `test native function`() = checkErrors("""
        module 0x1::M {
            native fun <error descr="Invalid function name: `assert` is a built-in function">assert</error>();
        }
    """)
}
