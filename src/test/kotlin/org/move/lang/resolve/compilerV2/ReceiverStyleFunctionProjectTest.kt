package org.move.lang.resolve.compilerV2

import org.move.utils.tests.MoveV2
import org.move.utils.tests.resolve.ResolveProjectTestCase

@MoveV2()
class ReceiverStyleFunctionProjectTest: ResolveProjectTestCase() {

    fun `test resolve vector method if stdlib vector module present`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move("vector.move", """
        module 0x1::vector {
            public native fun length<T>(self: &vector<T>): u8;
                               //X
        }        
            """)
            main("""
        module 0x1::main {
            fun main() {
                vector[1].length();
                          //^ 
            }
        }
            """)
        }
    }

    fun `test resolve vector reference method if stdlib vector module present`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move("vector.move", """
        module 0x1::vector {
            public native fun length<T>(self: &vector<T>): u8;
                               //X
        }        
            """)
            main("""
        module 0x1::main {
            fun main() {
                (&vector[1]).length();
                             //^ 
            }
        }
            """)
        }
    }

    fun `test vector method unresolved if no stdlib module`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            main("""
        module 0x1::main {
            fun main() {
                vector[1].length();
                          //^ unresolved
            }
        }
            """)
        }
    }

    fun `test vector method unresolved if address of vector module is different`() = checkByFileTree {
        namedMoveToml("MyPackage")
        sources {
            move("vector.move", """
        module 0x2::vector {
            public native fun length<T>(self: &vector<T>): u8;
        }        
            """)
            main("""
        module 0x1::main {
            fun main() {
                vector[1].length();
                          //^ unresolved
            }
        }
            """)
        }
    }
}