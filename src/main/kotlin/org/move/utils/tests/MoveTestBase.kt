/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.move.utils.tests.base.MoveTestCase
import org.move.utils.tests.base.TestCase

abstract class MoveTestBase : BasePlatformTestCase(),
                              MoveTestCase {
    protected val fileName: String
        get() = "${getTestName(true)}.$testFileExtension"
    open val dataPath: String = ""

    override fun getTestDataPath(): String = "${TestCase.testResourcesPath}/$dataPath"
    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    protected fun inlineFile(@Language("Move") code: String, name: String = "main.move"): InlineFile {
        return InlineFile(myFixture, code, name)
    }

//    protected fun checkAstNotLoaded(fileFilter: VirtualFileFilter) {
//        PsiManagerEx.getInstanceEx(project).setAssertOnFileLoadingFilter(fileFilter, testRootDisposable)
//    }

//    protected fun checkAstNotLoaded() {
//        PsiManagerEx.getInstanceEx(project)
//            .setAssertOnFileLoadingFilter(VirtualFileFilter.ALL, testRootDisposable)
//    }

    protected inline fun <reified T : PsiElement> findElementInEditor(marker: String = "^"): T =
        findElementInEditor(T::class.java, marker)

    protected fun <T : PsiElement> findElementInEditor(psiClass: Class<T>, marker: String): T {
        val (element, data) = findElementWithDataAndOffsetInEditor(psiClass, marker)
        check(data.isEmpty()) { "Did not expect marker data" }
        return element
    }

    protected inline fun <reified T : PsiElement> findElementWithDataAndOffsetInEditor(
        marker: String = "^",
    ): Triple<T, String, Int> {
        return findElementWithDataAndOffsetInEditor(T::class.java, marker)
    }

    protected fun <T : PsiElement> findElementWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String,
    ): Triple<T, String, Int> {
        val elementsWithDataAndOffset = findElementsWithDataAndOffsetInEditor(psiClass, marker)
        check(elementsWithDataAndOffset.isNotEmpty()) { "No `$marker` marker:\n${myFixture.file.text}" }
        check(elementsWithDataAndOffset.size <= 1) { "More than one `$marker` marker:\n${myFixture.file.text}" }
        return elementsWithDataAndOffset.first()
    }

    protected fun checkByText(
        @Language("Move") before: String,
        @Language("Move") after: String,
        action: () -> Unit,
    ) {
        inlineFile(before)
        action()
        myFixture.checkResult(replaceCaretMarker(after))
    }

    protected inline fun <reified T : PsiElement> findElementAndDataInEditor(marker: String = "^"): Pair<T, String> {
        val (element, data) = findElementWithDataAndOffsetInEditor<T>(marker)
        return element to data
    }

    protected inline fun <reified T : PsiElement> findElementAndOffsetInEditor(marker: String = "^"): Pair<T, Int> {
        val (element, _, offset) = findElementWithDataAndOffsetInEditor<T>(marker)
        return element to offset
    }

    private fun <T : PsiElement> findElementsWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String,
    ): List<Triple<T, String, Int>> {
        val commentPrefix =
            LanguageCommenters.INSTANCE.forLanguage(myFixture.file.language).lineCommentPrefix ?: "//"
        val caretMarker = "$commentPrefix$marker"
        val text = myFixture.file.text
        val triples = mutableListOf<Triple<T, String, Int>>()
        var markerOffset = -caretMarker.length
        while (true) {
            markerOffset = text.indexOf(caretMarker, markerOffset + caretMarker.length)
            if (markerOffset == -1) break
            val data = text.drop(markerOffset).removePrefix(caretMarker).takeWhile { it != '\n' }.trim()
            val markerPosition =
                myFixture.editor.offsetToLogicalPosition(markerOffset + caretMarker.length - 1)
            val previousLine = LogicalPosition(markerPosition.line - 1, markerPosition.column)
            val elementOffset = myFixture.editor.logicalPositionToOffset(previousLine)
            val elementAtMarker = myFixture.file.findElementAt(elementOffset)!!
            val element = PsiTreeUtil.getParentOfType(elementAtMarker, psiClass, false)
            if (element != null) {
                triples.add(Triple(element, data, elementOffset))
            } else {
                val injectionElement = InjectedLanguageManager.getInstance(project)
                    .findInjectedElementAt(myFixture.file, elementOffset)
                    ?.let { PsiTreeUtil.getParentOfType(it, psiClass, false) }
                    ?: error("No ${psiClass.simpleName} at `${elementAtMarker.text}`")
                val injectionOffset = (injectionElement.containingFile.virtualFile as VirtualFileWindow)
                    .documentWindow.hostToInjected(elementOffset)
                triples.add(Triple(injectionElement, data, injectionOffset))
            }
        }
        return triples
    }

    protected fun checkByFile(ignoreTrailingWhitespace: Boolean = true, action: () -> Unit) {
        val (before, after) = (fileName to fileName.replace(".move", "_after.move"))
        myFixture.configureByFile(before)
        action()
        myFixture.checkResultByFile(after, ignoreTrailingWhitespace)
    }

    protected fun FileTree.create(): TestProject = create(myFixture)
    protected fun FileTree.createAndOpenFileWithCaretMarker(): TestProject =
        createAndOpenFileWithCaretMarker(myFixture)

//    protected val PsiElement.lineNumber: Int
//        get() = myFixture.getDocument(myFixture.file).getLineNumber(textOffset)
}
