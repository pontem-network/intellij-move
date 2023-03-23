package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class StructFieldsNumberErrorTest: AnnotatorTestCase(MvErrorAnnotator::class) {
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

    fun `test missing fields for struct with alias`() = checkErrors("""
        module 0x1::string {
            struct T {
                field: u8
            }
        }
        module 0x1::M {
            use 0x1::string::T as MyT;
            fun main() {
                let a = <error descr="Some fields are missing">MyT</error> {};
                let <error descr="Some fields are missing">MyT</error> {} = call();
            }
        }    
    """)

}
