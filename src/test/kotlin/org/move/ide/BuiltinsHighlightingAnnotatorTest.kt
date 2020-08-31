package org.move.ide

import org.move.ide.annotator.BuiltinTypesHighlightingAnnotator
import org.move.ide.colors.MoveColor
import org.move.utils.tests.annotator.AnnotatorTestCase

class BuiltinsHighlightingAnnotatorTest : AnnotatorTestCase(BuiltinTypesHighlightingAnnotator::class) {
    override fun setUp() {
        super.setUp()
        createAnnotatorFixture().registerSeverities(MoveColor.values().map(MoveColor::testSeverity))
    }

    fun `test builtin types highlighted as keywords`() = checkHighlighting(
        """
        script {
            fun main(s: &<PRIMITIVE_TYPE>signer</PRIMITIVE_TYPE>,
                     val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>,
                     val2: <PRIMITIVE_TYPE>u64</PRIMITIVE_TYPE>,
                     val3: <PRIMITIVE_TYPE>u128</PRIMITIVE_TYPE>,
                     val4: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>
                     ) {}
        }
    """
    )
//    fun `test builtin types highlighted as keywords`() = checkHighlighting("""
//        <KEYWORD>script</KEYWORD> {
//            <KEYWORD>fun</KEYWORD> main(s: &<DEFAULT_KEYWORD>signer</DEFAULT_KEYWORD>, val: <DEFAULT_KEYWORD>u8</DEFAULT_KEYWORD>) {}
//        }
//    """)
}