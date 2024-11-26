package org.move.ide.annotator

import org.move.ide.colors.MvColor
import org.move.utils.tests.annotation.AnnotatorTestCase

class AttrHighlightingAnnotatorTest: AnnotatorTestCase(HighlightingAnnotator::class) {
    override fun setUp() {
        super.setUp()
        annotationFixture.registerSeverities(MvColor.entries.map(MvColor::testSeverity))
    }

    fun `test highlight annotator identifier`() = checkHighlighting("""
        module 0x1::m {
            <ATTRIBUTE>#</ATTRIBUTE><ATTRIBUTE>[</ATTRIBUTE><ATTRIBUTE>view</ATTRIBUTE><ATTRIBUTE>]</ATTRIBUTE>
            fun foo() {}
        }        
    """)

    fun `test highlight annotator fq name`() = checkHighlighting("""
        module 0x1::m {
            <ATTRIBUTE>#</ATTRIBUTE><ATTRIBUTE>[</ATTRIBUTE><ATTRIBUTE>lint</ATTRIBUTE><ATTRIBUTE>::</ATTRIBUTE><ATTRIBUTE>view</ATTRIBUTE><ATTRIBUTE>]</ATTRIBUTE>
            fun foo() {}
        }        
    """)

    fun `test highlight annotator initializer`() = checkHighlighting("""
        module 0x1::m {
            <ATTRIBUTE>#</ATTRIBUTE><ATTRIBUTE>[</ATTRIBUTE><ATTRIBUTE>test</ATTRIBUTE><ATTRIBUTE>(</ATTRIBUTE><ATTRIBUTE>signer</ATTRIBUTE> <ATTRIBUTE>=</ATTRIBUTE> <ADDRESS>@0x1</ADDRESS>)<ATTRIBUTE>]</ATTRIBUTE>
            fun foo() {}
        }        
    """)
}