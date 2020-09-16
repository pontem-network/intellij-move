package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class ParametersNumberErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test valid number of parameters`() = checkErrors("""
        module M {
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

    fun `test invalid number of parameters`() = checkErrors("""
        module M {
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
}