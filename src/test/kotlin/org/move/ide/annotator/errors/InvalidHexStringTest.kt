package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class InvalidHexStringTest: AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test valid hex strings`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                x""; x"11"; x"ff";
            }
        }        
    """
    )

    fun `test invalid hex strings`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                x"1"; 
                x"f";
                x"acaca";
                x"ii";
                x"fffz";
            }
        }        
    """
    )
}
