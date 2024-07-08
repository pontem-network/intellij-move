package org.move.ide.annotator.syntaxErrors.compilerV2

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.ide.inspections.fixes.CompilerV2Feat.RECEIVER_STYLE_FUNCTIONS
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.annotation.AnnotatorTestCase

class ReceiverStyleFunctionsTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    @CompilerV2Features()
    fun `test cannot use receiver style functions in compiler v1`() = checkWarnings("""
        module 0x1::m {
            struct S { field: u8 }
            fun receiver(self: &S): u8 { self.field }
            fun call(s: S) {
                s.<error descr="receiver-style functions are not supported in Aptos Move V1">receiver()</error>;
            }
        }        
    """)

    @CompilerV2Features(RECEIVER_STYLE_FUNCTIONS)
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