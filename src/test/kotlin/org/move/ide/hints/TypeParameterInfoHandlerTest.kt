package org.move.ide.hints

import org.move.lang.core.psi.MvTypeArgumentList
import org.move.utils.tests.ParameterInfoHandlerTestCase

class TypeParameterInfoHandlerTest :
    ParameterInfoHandlerTestCase<MvTypeArgumentList, TypeParamsDescription>(TypeParameterInfoHandler()) {

    fun `test struct as type`() = checkByText("""
        module M {
            struct S<T: copy> {
                field: T
            }
            
            fun main(val: S</*caret*/>) {}
        } 
    """, "T: copy", 0)

    fun `test struct as literal`() = checkByText("""
        module M {
            struct S<T: copy> {
                field: T
            }
            
            fun main() {
                let a = S</*caret*/> {}
            }
        } 
    """, "T: copy", 0)

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
            fun call<R: store>() {}
            
            fun main() {
                call</*caret*/>()
            }
        } 
    """, "R: store", 0)

    fun `test aliased function`() = checkByText("""
        module 0x1::mod {
            public fun call<R: store>() {}
        }
        module M {
            use 0x1::mod::call as mycall;
            fun main() {
                mycall</*caret*/>()
            }
        } 
    """, "R: store", 0)

    fun `test function index 0`() = checkByText("""
        module M {
            fun call<R: store, S: copy>() {}
            
            fun main() {
                call<u8/*caret*/, u8>()
            }
        } 
    """, "R: store, S: copy", 0)

    fun `test function index 1`() = checkByText("""
        module M {
            fun call<R: store, S: copy>() {}
            
            fun main() {
                call<u8, u8/*caret*/>()
            }
        } 
    """, "R: store, S: copy", 1)
}
