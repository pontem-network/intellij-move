package org.move.ide.annotator.syntaxErrors

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class NativeStructNotSupportedTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    fun `test native struct is not supported by the vm`() = checkWarnings("""
        module 0x1::m {
            <error descr="Native structs aren't supported by the Move VM anymore">native struct</error> S;
        }        
    """)
}