package org.move.ide.annotator.syntaxErrors.compilerV2

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.AnnotatorTestCase

class ReceiverStyleFunctionsTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    @MoveV2(enabled = false)
    fun `test cannot use receiver style functions in compiler v1`() = checkWarnings("""
        module 0x1::m {
            struct S { field: u8 }
            fun receiver(self: &S): u8 { self.field }
            fun call(s: S) {
                s.<error descr="receiver-style functions are not supported in Aptos Move V1">receiver()</error>;
            }
        }        
    """)

    @MoveV2()
    fun `test receiver style functions in compiler v2`() = checkWarnings("""
        module 0x1::m {
            struct S { field: u8 }
            fun receiver(self: &S): u8 { self.field }
            fun call(s: S) {
                s.receiver();
            }
        }        
    """)
}