package org.move.ide.search

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.ext.startOffset
import org.move.utils.tests.MoveTestCase

class FindUsagesTest : MoveTestCase() {
    fun `test let declaration usages`() = doTestByText("""
        script {
            fun main() {
                let z = 1;
                  //^
                let z = z + 1;// - expr
                z;
            }
        }
    """)

    fun `test function params usages`() = doTestByText("""
        script {
            fun main(z: u8) {
                   //^
                let y = z * 2;// - expr
                let z = 2;
                z;
            }
        }    
    """)

    private fun doTestByText(@Language("Move") code: String) {
        InlineFile(code)

        val (_, _, offset) = findElementWithDataAndOffsetInEditor<PsiElement>()
        val source = TargetElementUtil.getInstance().findTargetElement(
            myFixture.editor,
            TargetElementUtil.ELEMENT_NAME_ACCEPTED,
            offset
        ) as? MoveNamedElement ?: error("Element not found")

        val actual = markersActual(source)
        val expected = markersFrom(code)
        assertEquals(expected.joinToString(COMPARE_SEPARATOR), actual.joinToString(COMPARE_SEPARATOR))
    }

    private fun markersActual(source: MoveNamedElement) =
        myFixture.findUsages(source)
            .filter { it.element != null }
            .map { Pair(it.element?.line ?: -1, MoveUsageTypeProvider.getUsageType(it.element).toString()) }
            .sortedBy { it.first }

    private fun markersFrom(text: String) =
        text.split('\n')
            .withIndex()
            .filter { it.value.contains(MARKER) }
            .map { Pair(it.index, it.value.substring(it.value.indexOf(MARKER) + MARKER.length).trim()) }

    private companion object {
        const val MARKER = "// - "
        const val COMPARE_SEPARATOR = " | "
    }

    private val PsiElement.line: Int? get() = containingFile.viewProvider.document?.getLineNumber(startOffset)
}