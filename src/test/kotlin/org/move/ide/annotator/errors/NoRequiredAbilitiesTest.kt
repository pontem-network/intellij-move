package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class NoRequiredAbilitiesTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test fields of struct should have abilities of struct`() = checkErrors("""
    module M {
        struct A {}
        
        struct B has copy {
            <error descr="The type 'A' does not have the ability 'copy' required by the declared ability 'copy' of the struct 'B'">a: A</error>
        }
    }    
    """)

    fun `test key struct requires store fields`() = checkErrors("""
    module M {
        struct A {}
        
        struct B has key {
            <error descr="The type 'A' does not have the ability 'store' required by the declared ability 'key' of the struct 'B'">a: A</error>
        }
    }    
    """)
}
