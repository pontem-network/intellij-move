package org.move.utils.tests.base

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

inline fun <reified T : PsiElement> CodeInsightTestFixture.findElementInEditor(marker: String = "^"): T =
    findElementInEditor(T::class.java, marker)

fun <T : PsiElement> CodeInsightTestFixture.findElementInEditor(psiClass: Class<T>, marker: String): T {
    val (element, data) = findElementWithDataAndOffsetInEditor(psiClass, marker)
    check(data.isEmpty()) { "Did not expect marker data" }
    return element
}

inline fun <reified T : PsiElement> CodeInsightTestFixture.findElementWithDataAndOffsetInEditor(
    marker: String = "^",
): Triple<T, String, Int> {
    return findElementWithDataAndOffsetInEditor(T::class.java, marker)
}

inline fun <reified T : PsiElement> CodeInsightTestFixture.findElementAndDataInEditor(marker: String = "^"): Pair<T, String> {
    val (element, data) = findElementWithDataAndOffsetInEditor<T>(marker)
    return element to data
}

inline fun <reified T : PsiElement> CodeInsightTestFixture.findElementAndOffsetInEditor(marker: String = "^"): Pair<T, Int> {
    val (element, _, offset) = findElementWithDataAndOffsetInEditor<T>(marker)
    return element to offset
}

fun <T : PsiElement> CodeInsightTestFixture.findElementWithDataAndOffsetInEditor(
    psiClass: Class<T>,
    marker: String,
): Triple<T, String, Int> {
    val elementsWithDataAndOffset = findElementsWithDataAndOffsetInEditor(psiClass, marker)
    check(elementsWithDataAndOffset.isNotEmpty()) { "No `$marker` marker:\n${this.file.text}" }
    check(elementsWithDataAndOffset.size <= 1) { "More than one `$marker` marker:\n${this.file.text}" }
    return elementsWithDataAndOffset.first()
}

fun <T : PsiElement> CodeInsightTestFixture.findElementsWithDataAndOffsetInEditor(
    psiClass: Class<T>,
    marker: String,
): List<Triple<T, String, Int>> {
    val commentPrefix =
        LanguageCommenters.INSTANCE.forLanguage(this.file.language).lineCommentPrefix ?: "//"
    val caretMarker = "$commentPrefix$marker"
    val text = this.file.text
    val triples = mutableListOf<Triple<T, String, Int>>()
    var markerOffset = -caretMarker.length
    while (true) {
        markerOffset = text.indexOf(caretMarker, markerOffset + caretMarker.length)
        if (markerOffset == -1) break
        val data = text.drop(markerOffset).removePrefix(caretMarker).takeWhile { it != '\n' }.trim()
        val markerPosition =
            this.editor.offsetToLogicalPosition(markerOffset + caretMarker.length - 1)
        val previousLine = LogicalPosition(markerPosition.line - 1, markerPosition.column)
        val elementOffset = this.editor.logicalPositionToOffset(previousLine)
        val elementAtMarker = this.file.findElementAt(elementOffset)!!
        val element = PsiTreeUtil.getParentOfType(elementAtMarker, psiClass, false)
        if (element != null) {
            triples.add(Triple(element, data, elementOffset))
        } else {
            val injectionElement = InjectedLanguageManager.getInstance(project)
                .findInjectedElementAt(this.file, elementOffset)
                ?.let { PsiTreeUtil.getParentOfType(it, psiClass, false) }
                ?: error("No ${psiClass.simpleName} at `${elementAtMarker.text}`")
            val injectionOffset = (injectionElement.containingFile.virtualFile as VirtualFileWindow)
                .documentWindow.hostToInjected(elementOffset)
            triples.add(Triple(injectionElement, data, injectionOffset))
        }
    }
    return triples
}


interface MoveTestCase : TestCase {
    override val testFileExtension: String get() = "move"
}
