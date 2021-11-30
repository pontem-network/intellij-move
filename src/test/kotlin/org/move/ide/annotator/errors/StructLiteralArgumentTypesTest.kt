package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class StructLiteralArgumentTypesTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test invalid type for field in struct literal`() = checkErrors("""
    module 0x1::M {
        struct Deal { val: u8 }
        fun main() {
            Deal { val: <error descr="Invalid argument for field 'val': type 'bool' is not compatible with 'u8'">false</error> };
        }
    }    
    """)

    fun `test valid type for field`() = checkErrors("""
    module 0x1::M {
        struct Deal { val: u8 }
        fun main() {
            Deal { val: 10 };
            Deal { val: 10u8 };
        }
    }    
    """)

    fun `test no need for explicit type parameter if inferrable from context`() = checkErrors("""
    module 0x1::M {
        struct Option<Element> has copy, drop, store {}
        public fun none<Element>(): Option<Element> {
            Option {}
        }
        struct S { field: Option<address> }
        fun m(): S {
            S { field: none() }
        }
        
    }
    """)

    fun `test no need for vector empty() generic`() = checkErrors("""
    module 0x1::M {
        /// Create an empty vector.
        native public fun empty<Element>(): vector<Element>;
        struct CapState<phantom Feature> has key {
            delegates: vector<address>
        }
        fun m() {
            CapState { delegates: empty() };
        }
    }    
    """)
}
