package org.move.ide.annotator.syntaxErrors

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class EntryFunCannotHaveReturnValueTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    fun `test entry fun without return value`() = checkWarnings("""
        module 0x1::m {
            public entry fun transfer() {}
            public entry fun transfer_params(a: u8, b: u8) {}
        }
    """)

    fun `test error if entry function has return value`() = checkWarnings("""
        module 0x1::m {
            public entry fun transfer()<error descr="Entry functions cannot have a return value">: u8</error> {}
            public entry fun transfer_params(a: u8, b: u8)<error descr="Entry functions cannot have a return value">: (u8, u8)</error> {}
        }
    """)

    fun `test no error if test_only function`() = checkWarnings("""
        module 0x1::m {
            #[test_only]
            public entry fun main(): u8 {}
        }
    """)

    fun `test no error if test function`() = checkWarnings("""
        module 0x1::m {
            #[test]
            public entry fun main(): u8 {}
        }
    """)
}
