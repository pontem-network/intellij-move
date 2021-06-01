package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class TypeSubstitutionTest: TypificationTestCase() {
    fun `test return type of callable`() = testExpr("""
    module M {
        fun call<R>(): R {}
        fun main() {
            call<u8>()
          //^ u8  
        }
    }    
    """)
}
