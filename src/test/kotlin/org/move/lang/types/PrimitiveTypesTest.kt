package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class PrimitiveTypesTest: TypificationTestCase() {
    fun `test default int is u8`() = testExpr("""
        script {
            fun main() {
                let a = 1;
                      //^ integer
            }
        }
    """)

    fun `test u8 integer`() = testExpr("""
        script {
            fun main() {
                let a = 1u8;
                      //^ u8
            }
        }
    """)

    fun `test u64 integer`() = testExpr("""
        script {
            fun main() {
                let a = 1u64;
                      //^ u64
            }
        }
    """)

    fun `test u128 integer`() = testExpr("""
        script {
            fun main() {
                let a = 1u128;
                      //^ u128
            }
        }
    """)

    fun `test boolean true`() = testExpr("""
        script {
            fun main() {
                let a = false;
                      //^ bool
            }
        }
    """)

    fun `test boolean false`() = testExpr("""
        script {
            fun main() {
                let a = true;
                      //^ bool
            }
        }
    """)

    fun `test address`() = testExpr("""
        script {
            fun main() {
                let a = @0xBEEF;
                      //^ address
            }
        }
    """)

    fun `test bytestring is vector of u8`() = testExpr("""
        script {
            fun main() {
                let a = b"deadbeef";
                      //^ vector<u8>
            }
        }
    """)
}
