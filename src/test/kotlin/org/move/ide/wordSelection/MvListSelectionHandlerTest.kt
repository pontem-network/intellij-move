/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.wordSelection

import org.move.utils.tests.MvSelectionHandlerTestBase

class MvListSelectionHandlerTest : MvSelectionHandlerTestBase() {

    fun `test function value parameters list`() = doTest("""
        module 0x1::m {
            fun main(a: u3<caret>2, b: bool) {}
        }
    """, """
        module 0x1::m {
            fun main(a: <selection>u3<caret>2</selection>, b: bool) {}
        }
    """, """
        module 0x1::m {
            fun main(a<selection>: u3<caret>2</selection>, b: bool) {}
        }
    """, """
        module 0x1::m {
            fun main(<selection>a: u3<caret>2</selection>, b: bool) {}
        }
    """, """
        module 0x1::m {
            fun main(<selection>a: u3<caret>2, b: bool</selection>) {}
        }
    """, """
        module 0x1::m {
            fun main<selection>(a: u3<caret>2, b: bool)</selection> {}
        }
    """)

    fun `test function type parameters list`() = doTest("""
        module 0x1::m {
            fun add<T, U: dr<caret>op + store>() {} 
        }
    """, """
        module 0x1::m {
            fun add<T, U: <selection>dr<caret>op</selection> + store>() {} 
        }
    """, """
        module 0x1::m {
            fun add<T, U: <selection>dr<caret>op + store</selection>>() {} 
        }
    """, """
        module 0x1::m {
            fun add<T, U<selection>: dr<caret>op + store</selection>>() {} 
        }
    """, """
        module 0x1::m {
            fun add<T, <selection>U: dr<caret>op + store</selection>>() {} 
        }
    """, """
        module 0x1::m {
            fun add<<selection>T, U: dr<caret>op + store</selection>>() {} 
        }
    """, """
        module 0x1::m {
            fun add<selection><T, U: dr<caret>op + store></selection>() {} 
        }
    """)

    fun `test function value arguments list`() = doTest("""
        module 0x1::m {
            fun main() {
                call(1<caret>4, 2);
            }
        }
    """, """
        module 0x1::m {
            fun main() {
                call(<selection>1<caret>4</selection>, 2);
            }
        }
    """, """
        module 0x1::m {
            fun main() {
                call(<selection>1<caret>4, 2</selection>);
            }
        }
    """, """
        module 0x1::m {
            fun main() {
                call<selection>(1<caret>4, 2)</selection>;
            }
        }
    """)

    fun `test function type arguments list`() = doTest("""
        module 0x1::m {
            fun main(): Result<u32, bo<caret>ol> {}
        }
    """, """
        module 0x1::m {
            fun main(): Result<u32, <selection>bo<caret>ol</selection>> {}
        }
    """, """
        module 0x1::m {
            fun main(): Result<<selection>u32, bo<caret>ol</selection>> {}
        }
    """, """
        module 0x1::m {
            fun main(): Result<selection><u32, bo<caret>ol></selection> {}
        }
    """)
}
