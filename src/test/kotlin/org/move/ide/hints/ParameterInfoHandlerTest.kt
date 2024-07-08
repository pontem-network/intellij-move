package org.move.ide.hints

import org.move.ide.inspections.fixes.CompilerV2Feat.RECEIVER_STYLE_FUNCTIONS
import org.move.lang.core.psi.MvValueArgumentList
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.ParameterInfoHandlerTestCase

class ParameterInfoHandlerTest
    : ParameterInfoHandlerTestCase<MvValueArgumentList, ParamsDescription>(FunctionParameterInfoHandler()) {

    fun `test fun no args`() = checkByText(
        """
        module M {
            fun foo() {}
            fun main() { foo(/*caret*/); }    
        }
    """, "<no arguments>", 0
    )

    fun `test fun no args before args`() = checkByText(
        """
        module M {
            fun foo() {}
            fun main() { foo/*caret*/(); }    
        }
    """, "<no arguments>", -1
    )

    fun `test fun one arg`() = checkByText(
        """
        module M {
            fun foo(arg: u8) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8", 0
    )

    fun `test fun one arg end`() = checkByText(
        """
        module M {
            fun foo(arg: u8) {}
            fun main() { foo(42/*caret*/); }    
        }
    """, "arg: u8", 0
    )

    fun `test fun many args`() = checkByText(
        """
        module M {
            fun foo(arg: u8, s: &signer, v: vector<u8>) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8, s: &signer, v: vector<u8>", 0
    )

    fun `test fun many args vector u8`() = checkByText(
        """
        module 0x1::M {
            fun call(a: u8, b: vector<u8>, c: vector<u8>) {}
            fun m() {
                call(1, b"11", b"22"/*caret*/);
            }
        }    
    """, "a: u8, b: vector<u8>, c: vector<u8>", 2
    )

    fun `test fun poorly formatted args`() = checkByText(
        """
        module M {
            fun foo(arg:          u8,     s:    &signer,    v   : vector<u8>) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8, s: &signer, v: vector<u8>", 0
    )

    fun `test fun args index 0`() = checkByText(
        """
        module M {
            fun foo(val1: u8, val2: u8) {}
            fun main() { foo(42/*caret*/); }    
        }
    """, "val1: u8, val2: u8", 0
    )

    fun `test fun args index 1`() = checkByText(
        """
        module M {
            fun foo(val1: u8, val2: u8) {}
            fun main() { foo(42, 10/*caret*/); }    
        }
    """, "val1: u8, val2: u8", 1
    )

    fun `test multiline call`() = checkByText(
        """
        module M {
            fun foo(val1: u8, val2: u8) {}
            fun main() { 
                foo(
                    42/*caret*/, 
                    10
                ); 
            }    
        }
    """, "val1: u8, val2: u8", 0
    )

    fun `test builtin function`() = checkByText(
        """
        module M {
            fun main() {
                borrow_global(/*caret*/);
            }    
        }
    """, "addr: address", 0
    )

    fun `test aliased function`() = checkByText(
        """
        module 0x1::string {
            public fun call(addr: address) {}
        }
        module M {
            use 0x1::string::call as mycall;
            fun main() {
                mycall(/*caret*/);
            }    
        }
    """, "addr: address", 0
    )

    fun `test not applied within declaration`() = checkByText(
        """
        module M {
            fun foo(val1/*caret*/: u8, val2: u8) {}
        }
    """, "", -1
    )

    fun `test fun incomplete args index 1`() = checkByText(
        """
        module 0x1::m {
            fun call(val1: u8, val2: u8) {}
            fun main() { call(42, /*caret*/); }    
        }
    """, "val1: u8, val2: u8", 1
    )

    fun `test fun incomplete args index 2`() = checkByText(
        """
        module 0x1::m {
            fun call(val1: u8, val2: u8, val3: u8) {}
            fun main() { call(42, 10, /*caret*/); }    
        }
    """, "val1: u8, val2: u8, val3: u8", 2
    )

    @CompilerV2Features(RECEIVER_STYLE_FUNCTIONS)
    fun `test receiver style fun`() = checkByText(
        """
        module 0x1::m {
            struct S { val: u8 }
            fun get_val(self: &S, modifier: bool): u8 { self.val }
            fun main(s: S) {
                s.get_val(/*caret*/);
            }
        }        
    """, "modifier: bool", 0
    )
}
