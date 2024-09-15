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
                let <error descr="Struct pattern does not mention field `field`">T</error> {} = call();
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
                let <error descr="Struct pattern does not mention field `field`">MyT</error> {} = call();
            }
        }    
    """)

    fun `test pattern with rest`() = checkErrors("""
        module 0x1::m {
            struct S { f1: u8, f2: u8 }
            fun main(s: S) {
                let S { f1, .. } = s;
            }
        }        
    """)

    fun `test missing fields for enum variant pattern`() = checkErrors("""
        module 0x1::M {
            enum Num { One { val: u8 }, Two { val: u8, val2: u8 }}

            fun main(s: Num) {
                match (s) {
                    <error descr="Enum variant pattern does not mention field `val2`">Two</error> { val } => true
                };
            }
        }    
    """)

}
