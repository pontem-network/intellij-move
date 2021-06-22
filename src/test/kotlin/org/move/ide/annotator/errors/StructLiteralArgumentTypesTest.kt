package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class StructLiteralArgumentTypesTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test invalid type for field in struct literal`() = checkErrors("""
    module M {
        struct Deal { val: u8 }
        fun main() {
            Deal { val: <error descr="Invalid argument for field 'val': type 'bool' is not compatible with 'u8'">false</error> };
        }
    }    
    """)

    fun `test valid type for field`() = checkErrors("""
    module M {
        struct Deal { val: u8 }
        fun main() {
            Deal { val: 10 };
            Deal { val: 10u8 };
        }
    }    
    """)
}
