package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class InvalidIfElseTypesTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test if condition should be boolean`() = checkErrors("""
    module M {
        fun m() {
            if (<error descr="Incompatible type 'integer', expected 'bool'">1</error>) 1;
        }
    }    
    """)

    fun `test incompatible types from branches`() = checkErrors("""
    module M {
        fun m() {
            if (true) {1} else {<error descr="Incompatible type 'bool', expected 'integer'">true</error>};
        }
    }    
    """)
}
