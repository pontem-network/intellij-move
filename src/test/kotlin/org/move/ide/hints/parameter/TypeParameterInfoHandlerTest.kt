package org.move.ide.hints.parameter

import org.move.lang.core.psi.MoveTypeArgumentList
import org.move.utils.tests.ParameterInfoHandlerTestCase

class TypeParameterInfoHandlerTest :
    ParameterInfoHandlerTestCase<MoveTypeArgumentList, TypeParametersDescription>(TypeParameterInfoHandler()) {

    fun `test struct as type`() = checkByText("""
        module M {
            struct S<T: copyable> {
                field: T
            }
            
            fun main(val: S</*caret*/>) {}
        } 
    """, "T: copyable", 0)

    fun `test struct as literal`() = checkByText("""
        module M {
            struct S<T: copyable> {
                field: T
            }
            
            fun main() {
                let a = S</*caret*/> {}
            }
        } 
    """, "T: copyable", 0)

    fun `test function no arguments`() = checkByText("""
        module M {
            fun call() {}
            
            fun main() {
                call</*caret*/>()
            }
        } 
    """, "<no arguments>", 0)

    fun `test function`() = checkByText("""
        module M {
            fun call<R: resource>() {}
            
            fun main() {
                call</*caret*/>()
            }
        } 
    """, "R: resource", 0)

    fun `test function index 0`() = checkByText("""
        module M {
            fun call<R: resource, S: copyable>() {}
            
            fun main() {
                call<u8/*caret*/, u8>()
            }
        } 
    """, "R: resource, S: copyable", 0)

    fun `test function index 1`() = checkByText("""
        module M {
            fun call<R: resource, S: copyable>() {}
            
            fun main() {
                call<u8, u8/*caret*/>()
            }
        } 
    """, "R: resource, S: copyable", 1)
}