package org.move.ide.hints

import com.intellij.psi.PsiElement
import org.move.ide.hints.paramInfo.CompositeParameterInfoHandler
import org.move.ide.hints.paramInfo.ParameterInfoProvider
import org.move.ide.hints.paramInfo.ParameterInfoProvider.ParametersInfo
import org.move.utils.tests.MoveV2
import org.move.utils.tests.ParameterInfoHandlerTestCase

class ParameterInfoHandlerTest:
    ParameterInfoHandlerTestCase<PsiElement, ParametersInfo>(CompositeParameterInfoHandler()) {

    fun `test fun no args`() = checkByText(
        """
        module 0x1::m {
            fun foo() {}
            fun main() { foo(/*caret*/); }    
        }
    """, "<no arguments>", 0
    )

    fun `test fun no args before args`() = checkByText(
        """
        module 0x1::m {
            fun foo() {}
            fun main() { foo/*caret*/(); }    
        }
    """, "<no arguments>", -1
    )

    fun `test fun one arg`() = checkByText(
        """
        module 0x1::m {
            fun foo(arg: u8) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8", 0
    )

    fun `test fun one arg end`() = checkByText(
        """
        module 0x1::m {
            fun foo(arg: u8) {}
            fun main() { foo(42/*caret*/); }    
        }
    """, "arg: u8", 0
    )

    fun `test fun many args`() = checkByText(
        """
        module 0x1::m {
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
        module 0x1::m {
            fun foo(arg:          u8,     s:    &signer,    v   : vector<u8>) {}
            fun main() { foo(/*caret*/); }    
        }
    """, "arg: u8, s: &signer, v: vector<u8>", 0
    )

    fun `test fun args index 0`() = checkByText(
        """
        module 0x1::m {
            fun foo(val1: u8, val2: u8) {}
            fun main() { foo(42/*caret*/); }    
        }
    """, "val1: u8, val2: u8", 0
    )

    fun `test fun args index 1`() = checkByText(
        """
        module 0x1::m {
            fun foo(val1: u8, val2: u8) {}
            fun main() { foo(42, 10/*caret*/); }    
        }
    """, "val1: u8, val2: u8", 1
    )

    fun `test multiline call`() = checkByText(
        """
        module 0x1::m {
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
        module 0x1::m {
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
        module 0x1::m {
            use 0x1::string::call as mycall;
            fun main() {
                mycall(/*caret*/);
            }    
        }
    """, "addr: address", 0
    )

    fun `test not applied within declaration`() = checkByText(
        """
        module 0x1::m {
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

    @MoveV2()
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

    @MoveV2()
    fun `test receiver style fun called as a function`() = checkByText(
        """
        module 0x1::m {
            struct S { val: u8 }
            fun get_val(self: &S, modifier: bool): u8 { self.val }
            fun main(s: S) {
                get_val(/*caret*/);
            }
        }        
    """, "self: &S, modifier: bool", 0
    )

    @MoveV2()
    fun `test receiver style function with generic parameter`() = checkByText(
        """
        module 0x1::m {
            struct S<R> {}
            fun push_back<R>(self: &mut S<R>, e: R);
            fun main(s: S<u8>) {
                s.push_back(/*caret*/);
            }
        }        
    """, "e: u8", 0
    )

    fun `test parameter info for assert macro`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                assert!(/*caret*/);
            }
        }        
    """, "_: bool, err: u64", 0
    )

    @MoveV2
    fun `test parameter info for named tuple struct`() = checkByText("""
        module 0x1::m {
            struct S(u8, u16);
            fun main() {
                S(/*caret*/);
            }
        }        
    """, "u8, u16", 0)

    @MoveV2
    fun `test parameter info for generic named tuple struct`() = checkByText("""
        module 0x1::m {
            struct S<T>(T, T);
            fun main() {
                S<u8>(/*caret*/);
            }
        }        
    """, "u8, u8", 0)

    @MoveV2
    fun `test parameter info for named tuple enum variant`() = checkByText("""
        module 0x1::m {
            enum S { One(u8, u8) }
            fun main() {
                S::One(/*caret*/);
            }
        }        
    """, "u8, u8", 0)

    @MoveV2
    fun `test parameter info for generic named tuple enum variant`() = checkByText("""
        module 0x1::m {
            enum S<T> { One(T, T) }
            fun main() {
                S<u8>::One(/*caret*/);
            }
        }        
    """, "u8, u8", 0)
}
