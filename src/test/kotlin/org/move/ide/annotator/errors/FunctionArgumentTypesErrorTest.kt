package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class FunctionArgumentTypesErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test incorrect type address passed where &signer is expected`() = checkErrors("""
        module M {
            fun send(account: &signer) {}
            
            fun main(addr: address) {
                send(<error descr="Invalid argument for parameter 'account': type 'address' is not compatible with '&signer'">addr</error>)
            }
        }        
    """)

    fun `test incorrect type u8 passed where &signer is expected`() = checkErrors("""
        module M {
            fun send(account: &signer) {}
            
            fun main(addr: u8) {
                send(<error descr="Invalid argument for parameter 'account': type 'u8' is not compatible with '&signer'">addr</error>)
            }
        }        
    """)

    fun `test no errors if same type`() = checkErrors("""
        module M {
            fun send(account: &signer) {}
            
            fun main(acc: &signer) {
                send(acc)
            }
        }        
    """)

    fun `test mutable reference compatible with immutable reference`() = checkErrors("""
    module M {
        struct Option<Element> {
            vec: vector<Element>
        }
        fun is_none<Element>(t: &Option<Element>): bool {
            true
        }
        fun main(opt: &mut Option<Element>) {
            is_none(opt)
        } 
    }    
    """)

    fun `test immutable reference is not compatible with mutable reference`() = checkErrors("""
    module M {
        struct Option<Element> {
            vec: vector<Element>
        }
        fun is_none<Element>(t: &mut Option<Element>): bool {
            true
        }
        fun main(opt: &Option<Element>) {
            is_none(<error descr="Invalid argument for parameter 't': type '&Option' is not compatible with '&mut Option'">opt</error>)
        } 
    }    
    """)
}
