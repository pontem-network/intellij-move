package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class InvalidIntegerTest : AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test valid integers`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                1;
                1u8; 1u16; 1u32; 1u64; 1u128; 1u256;
                0x1; 0xff; 0xFFF; 0xACACAFFF;
                0011;
            }
        }        
    """
    )

    fun `test invalid integers`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                0b11;
                0xRR;
                0x;
            }
        }        
    """
    )
}
