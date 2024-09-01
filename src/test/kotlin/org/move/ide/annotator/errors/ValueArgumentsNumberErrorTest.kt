package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.AnnotatorTestCase

class ValueArgumentsNumberErrorTest: AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test valid number of parameters`() = checkErrors("""
        module 0x1::M {
            fun params_0() {}
            fun params_1(val: u8) {}
            fun params_3(val: u8, val2: u64, s: bool) {}
            
            fun main() {
                params_0();
                params_1(1);
                params_3(1, 1, true);
            }
        }    
    """)

    fun `test invalid number of parameters local`() = checkErrors("""
        module 0x1::M {
            fun params_0() {}
            fun params_1(val: u8) {}
            fun params_3(val: u8, val2: u64, s: &signer) {}

            fun main() {
                params_0(<error descr="This function takes 0 parameters but 1 parameter was supplied">4</error>);
                params_1(<error descr="This function takes 1 parameter but 0 parameters were supplied">)</error>;
                params_1(1, <error descr="This function takes 1 parameter but 2 parameters were supplied">4</error>);
                params_3(5, 1<error descr="This function takes 3 parameters but 2 parameters were supplied">)</error>;
            }
        }    
    """)

    @MoveV2()
    fun `test invalid number of parameters receiver style`() = checkErrors("""
        module 0x1::M {
            struct S { field: u8 }
            fun get_field_0(self: &S): u8 {}
            fun get_field_1(self: &S, a: u8): u8 {}
            fun get_field_3(self: &S, a: u8, b: u8, c: u8): u8 {}

            fun main(s: S) {
                s.get_field_0(<error descr="This function takes 0 parameters but 1 parameter was supplied">4</error>);
                s.get_field_1(<error descr="This function takes 1 parameter but 0 parameters were supplied">)</error>;
                s.get_field_1(1, <error descr="This function takes 1 parameter but 2 parameters were supplied">4</error>);
                s.get_field_3(5, 1<error descr="This function takes 3 parameters but 2 parameters were supplied">)</error>;
            }
        }    
    """)

    fun `test invalid number of parameters with import`() = checkErrors("""
        module 0x1::p {
            public fun params_3(val: u8, val2: u64, s: &signer) {}
        }
        module 0x1::M {
            use 0x1::p::params_3;
            fun main() {
                params_3(5, 1<error descr="This function takes 3 parameters but 2 parameters were supplied">)</error>;
            }
        }    
    """)

    fun `test invalid number of parameters with import alias`() = checkErrors("""
        module 0x1::p {
            public fun params_3(val: u8, val2: u64, s: &signer) {}
        }
        module 0x1::M {
            use 0x1::p::params_3 as params_alias;
            fun main() {
                params_alias(5, 1<error descr="This function takes 3 parameters but 2 parameters were supplied">)</error>;
            }
        }    
    """)

    fun `test lambda expr expected single parameter`() = checkErrors("""
        module 0x1::m {
            inline fun main<Element>(e: Element, f: |Element| u8) {
                f(<error descr="This function takes 1 parameter but 0 parameters were supplied">)</error>;
            }
        }        
    """)

    fun `test assert macro expects two parameters`() = checkErrors("""
        module 0x1::m {
            fun call() {
                assert!(true<error descr="This function takes 2 parameters but 1 parameter was supplied">)</error>;
                assert!(true, <error descr="This function takes 2 parameters but 1 parameter was supplied">)</error>;
                assert!(<error descr="This function takes 2 parameters but 0 parameters were supplied">)</error>;
            }
        }        
    """)
}
