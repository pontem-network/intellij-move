package org.move.ide.annotator.syntaxErrors.compilerV2

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.AnnotatorTestCase

class EnumMatchTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    @MoveV2(false)
    fun `test enum is not supported in v1`() = checkWarnings("""
        module 0x1::m {
            <error descr="Enums are not supported in Aptos Move V1">enum</error> S { One }
            
        }        
    """)

    @MoveV2(true)
    fun `test enums in v2`() = checkWarnings("""
        module 0x1::m {
            enum S { One }
            
        }        
    """)

    @MoveV2(false)
    fun `test match is not supported in v1`() = checkWarnings("""
        module 0x1::m {
            struct S {}
            fun main(s: S) {
                <error descr="Match expressions are not supported in Aptos Move V1">match</error> (s) {}
            }            
        }        
    """)

    @MoveV2(true)
    fun `test match in v2`() = checkWarnings("""
        module 0x1::m {
            struct S {}
            fun main(s: S) {
                match (s) {}
            }            
        }        
    """)
}