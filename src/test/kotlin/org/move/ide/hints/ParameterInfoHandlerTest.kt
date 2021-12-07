package org.move.ide.hints

import org.move.lang.core.psi.MoveCallArguments
import org.move.utils.tests.ParameterInfoHandlerTestCase

class ParameterInfoHandlerTest
    : ParameterInfoHandlerTestCase<MoveCallArguments, ParamsDescription>(FunctionParameterInfoHandler()) {

    fun `test fun no args`() = checkByText("""
        module M {
            fun foo() {}
            fun main() { foo(/*caret*/); }    
        }
    """, "<no arguments>", 0)

    fun `test fun no args before args`() = checkByText("""
        module M {
            fun foo() {}
            fun main() { foo/*caret*/(); }    
        }
    """, "<no arguments>", -1)

    fun `test fun one arg`() = checkByText("""
        module M {
            fun foo(arg: u8) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8", 0)

    fun `test fun one arg end`() = checkByText("""
        module M {
            fun foo(arg: u8) {}
            fun main() { foo(42/*caret*/); }    
        }
    """, "arg: u8", 0)

    fun `test fun many args`() = checkByText("""
        module M {
            fun foo(arg: u8, s: &signer, v: vector<u8>) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8, s: &signer, v: vector<u8>", 0)

    fun `test fun poorly formatted args`() = checkByText("""
        module M {
            fun foo(arg:          u8,     s:    &signer,    v   : vector<u8>) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8, s: &signer, v: vector<u8>", 0)

    fun `test fun args index 0`() = checkByText("""
        module M {
            fun foo(val1: u8, val2: u8) {}
            fun main() { foo(42/*caret*/); }    
        }
    """, "val1: u8, val2: u8", 0)

    fun `test fun args index 1`() = checkByText("""
        module M {
            fun foo(val1: u8, val2: u8) {}
            fun main() { foo(42, 10/*caret*/); }    
        }
    """, "val1: u8, val2: u8", 1)

    fun `test multiline call`() = checkByText("""
        module M {
            fun foo(val1: u8, val2: u8) {}
            fun main() { 
                foo(
                    42/*caret*/, 
                    10
                ); 
            }    
        }
    """, "val1: u8, val2: u8", 0)

    fun `test builtin function`() = checkByText("""
        module M {
            fun main() {
                borrow_global(/*caret*/);
            }    
        }
    """, "addr: address", 0)

    fun `test not applied within declaration`() = checkByText("""
        module M {
            fun foo(val1/*caret*/: u8, val2: u8) {}
        }
    """, "", -1)
}
