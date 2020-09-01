package org.move.ide

import org.move.ide.annotator.BuiltinsHighlightingAnnotator
import org.move.ide.colors.MoveColor
import org.move.utils.tests.annotator.AnnotatorTestCase

class BuiltinsHighlightingAnnotatorTest : AnnotatorTestCase(BuiltinsHighlightingAnnotator::class) {
    override fun setUp() {
        super.setUp()
        createAnnotatorFixture().registerSeverities(MoveColor.values().map(MoveColor::testSeverity))
    }

    fun `test primitive types highlighed in type positions`() = checkHighlighting(
        """
        script {
            fun main(s: &<PRIMITIVE_TYPE>signer</PRIMITIVE_TYPE>,
                     val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>,
                     val2: <PRIMITIVE_TYPE>u64</PRIMITIVE_TYPE>,
                     val3: <PRIMITIVE_TYPE>u128</PRIMITIVE_TYPE>,
                     val4: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>,
                     val5: 0x0::Transaction::bool,
                     ) {
                        let signer = 1;
                     }
        }
    """
    )

    fun `test builtin functions highlighted in call positions`() = checkHighlighting(
        """
        script {
            fun move_to() {
                let move_to = 1;
                
                <BUILTIN_FUNCTION>move_to</BUILTIN_FUNCTION>();
                <BUILTIN_FUNCTION>move_from</BUILTIN_FUNCTION>();
                <BUILTIN_FUNCTION>borrow_global</BUILTIN_FUNCTION>();
                <BUILTIN_FUNCTION>borrow_global_mut</BUILTIN_FUNCTION>();
                <BUILTIN_FUNCTION>freeze</BUILTIN_FUNCTION>();
            }
        }
    """
    )
}