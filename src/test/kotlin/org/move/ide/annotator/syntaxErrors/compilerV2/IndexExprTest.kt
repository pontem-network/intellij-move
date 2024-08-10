package org.move.ide.annotator.syntaxErrors.compilerV2

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.ide.inspections.fixes.CompilerV2Feat.INDEXING
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.annotation.AnnotatorTestCase

class IndexExprTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    @CompilerV2Features()
    fun `test vector index expr in not allowed in main code with compiler v1`() = checkWarnings("""
        module 0x1::m {
            fun main() {
                let v = vector[1, 2];
                <error descr="Index operator is not supported in Aptos Move V1 outside specs">v[1]</error>;
            }
        }        
    """)

    @CompilerV2Features()
    fun `test resource index expr in not allowed in main code with compiler v1`() = checkWarnings("""
        module 0x1::m {
            fun main() {
                let v = vector[1, 2];
                <error descr="Index operator is not supported in Aptos Move V1 outside specs">v[1]</error>;
            }
        }        
    """)

    @CompilerV2Features(INDEXING)
    fun `test no error with index expr in compiler v2`() = checkWarnings("""
        module 0x1::m {
            struct S has key { field: u8 }
            fun main() {
                let v1 = vector[1, 2];
                v1[1];
                S[@0x1].field = 11;
            }
        }        
    """)

    fun `test no error for specs in compiler v1`() = checkWarnings("""
        module 0x1::m {
            spec module {
                invariant forall ind in 0..10: vec[ind] < 10;
            }
        }        
    """)
}