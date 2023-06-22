package org.move.ide.annotator.syntaxErrors

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class InvalidHexStringTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    fun `test valid hex strings`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                x""; x"11"; x"ff";
                x"00112233445566778899aabbccddeeff"
            }
        }        
    """
    )

    fun `test invalid hex strings`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                x"<error descr="Odd number of characters in hex string. Expected 2 hexadecimal digits for each byte">1</error>"; 
                x"<error descr="Odd number of characters in hex string. Expected 2 hexadecimal digits for each byte">f</error>";
                x"<error descr="Odd number of characters in hex string. Expected 2 hexadecimal digits for each byte">acaca</error>";
                
                x"<error descr="Invalid hex symbol">i</error><error descr="Invalid hex symbol">i</error>";
                x"fff<error descr="Invalid hex symbol">z</error>";
            }
        }        
    """
    )
}
