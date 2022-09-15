package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class BuiltInFunctionNameErrorTest: AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test function`() = checkErrors("""
        module 0x1::M {
            fun <error descr="Invalid function name: `move_to` is a built-in function">move_to</error>() {}
        }
    """)

    fun `test native function`() = checkErrors("""
        module 0x1::M {
            native fun <error descr="Invalid function name: `move_to` is a built-in function">move_to</error>();
        }
    """)
}
