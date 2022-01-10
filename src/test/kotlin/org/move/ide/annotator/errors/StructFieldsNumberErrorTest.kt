package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class StructFieldsNumberErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test missing fields for struct`() = checkErrors("""
        module 0x1::M {
            struct T {
                field: u8
            }

            fun main() {
                let a = <error descr="Some fields are missing">T</error> {};
                let <error descr="Some fields are missing">T</error> {} = call();
            }
        }    
    """)

}
