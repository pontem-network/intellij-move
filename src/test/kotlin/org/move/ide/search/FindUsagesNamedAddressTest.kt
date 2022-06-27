package org.move.ide.search

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.ext.startOffset
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor
import org.toml.lang.psi.TomlKeySegment

class FindUsagesNamedAddressTest : MvProjectTestBase() {
    fun `test resolve toml to move usage`() = doTestByText(
        """
        //- Move.toml
        [addresses]
        Std = "0x1"
        #^
        //- sources/main.move
        module Std::Module {} // - address ref
    """
    )

    private fun doTestByText(@Language("Move") code: String) {
        testProject(code)

        val (_, _, offset) = myFixture.findElementWithDataAndOffsetInEditor<PsiElement>()
        val source = TargetElementUtil.getInstance().findTargetElement(
            myFixture.editor,
            TargetElementUtil.ELEMENT_NAME_ACCEPTED,
            offset
        ) as? TomlKeySegment ?: error("Element not found")

        val actual = markersActual(source)
        val expected = listOf(Pair(0, "address ref"))
        assertEquals(expected.joinToString(COMPARE_SEPARATOR), actual.joinToString(COMPARE_SEPARATOR))
    }

    private fun markersActual(source: TomlKeySegment) =
        myFixture.findUsages(source)
            .filter { it.element != null }
            .map {
                Pair(
                    it.element?.line ?: -1,
                    NamedAddressUsageTypeProvider().getUsageType(it.element!!).toString()
                )
            }
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
