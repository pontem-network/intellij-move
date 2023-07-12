package org.move.ide.annotator.syntaxErrors

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class InvalidIntegerTest : AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    fun `test valid integers`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                1;
                1u8; 1u16; 1u32; 1u64; 1u128; 1u256;
                0x123456789abcdef;
                0x1; 0xff; 0xFFF; 0xACACAFFF;
                0x1f1fu128;
                0011;
                
                0x1111_1111;
                1_000;
            }
        }        
    """
    )

    fun `test invalid hex integers`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                0x<error descr="Invalid hex integer symbol">R</error><error descr="Invalid hex integer symbol">R</error>;
                <error descr="Invalid hex integer">0x</error>;
            }
        }        
    """
    )

    fun `test invalid integers`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                0<error descr="Invalid integer symbol">b</error>11;
                012345<error descr="Invalid integer symbol">R</error><error descr="Invalid integer symbol">R</error>;

                0<error descr="Invalid integer suffix">u10</error>;
                0<error descr="Invalid integer suffix">u500</error>;
            }
        }        
    """
    )
}
