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

    fun `test function invocation with explicitly provided generic type`() = checkErrors("""
    module Event {
        struct Message has drop {}
        
        public fun emit_event<T: store + drop>() {}
        
        public fun main() {
            emit_event<<error descr="The type '0x1::Event::Message' does not have required ability 'store'">Message</error>>()
        }
    }    
    """)

    fun `test struct constructor with explicitly provided generic type`() = checkErrors("""
    module Event {
        struct Message has drop {}
        
        struct Event<Message: store + drop> {}
        
        public fun main() {
            Event<<error descr="The type '0x1::Event::Message' does not have required ability 'store'">Message</error>> {};
        }
    }    
    """)

    fun `test type param`() = checkErrors("""
    module Event {
        struct Message has drop {}
        
        public fun emit_event<T: store + drop>() {}
        
        public fun main<M: drop>() {
            emit_event<<error descr="The type 'M' does not have required ability 'store'">M</error>>()
        }
    }    
    """)


    fun `test no required ability 'key' for move_to argument`() = checkErrors("""
    module M {
        struct Res {}
        fun main(r: Res) {
            move_to<Res>(0x1, r)
        }
    }    
    """)

    fun `test no error in move_to with resource`() = checkErrors("""
    module M {
        struct Res has key {}
        fun main(r: Res) {
            move_to<Res>(0x1, r)
        }
    }    
    """)

    fun `test no required ability for struct for type param`() = checkErrors("""
    module M {
        struct Res {}
        fun save<T: key>(r: T) {}
        fun main(r: Res) {
            save(r)
        }
    }    
    """)

    fun `test no error in type param if structure has required abilities`() = checkErrors("""
    module M {
        struct Res has key {}
        fun save<T: key>(r: T) {}
        fun main(r: Res) {
            save(r)
        }
    }    
    """)
}
