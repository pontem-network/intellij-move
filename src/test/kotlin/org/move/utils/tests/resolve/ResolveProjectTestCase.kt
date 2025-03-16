package org.move.utils.tests.resolve

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.openapiext.document
import org.move.openapiext.toPsiFile
import org.move.openapiext.toVirtualFile
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.TestProject
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor
import kotlin.math.min

abstract class ResolveProjectTestCase: MvProjectTestBase() {
    open fun checkByFileTree(fileTree: FileTreeBuilder.() -> Unit) {
        checkByFileTree(
            MvReferenceElement::class.java,
            MvNamedElement::class.java,
            fileTree
        )
    }

    protected fun <T: PsiElement, R: PsiElement> checkByFileTree(
        refClass: Class<R>,
        targetClass: Class<T>,
        fileTree: FileTreeBuilder.() -> Unit
    ) {
        val testProject = testProject(fileTree)
        checkByTestProject(testProject, refClass, targetClass)
    }

    private fun <T: PsiElement, R: PsiElement> checkByTestProject(
        testProject: TestProject,
        refClass: Class<R>,
        targetClass: Class<T>
    ) {
        val (refElement, data, offset) =
            myFixture.findElementWithDataAndOffsetInEditor(refClass, "^")
        if (data == "unresolved") {
            val resolved = refElement.reference?.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }
        val resolved = refElement.checkedResolve(offset)

        val fileWithNamedElement =
            testProject.rootDirectory.toNioPath()
                .resolve(testProject.fileWithNamedElement).toVirtualFile()
                ?.toPsiFile(this.project)
                ?: error("No file with //X caret")

        val target = findElementInFile(fileWithNamedElement, targetClass, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }

    private fun <T: PsiElement> findElementInFile(file: PsiFile, psiClass: Class<T>, marker: String): T {
        val (element, data, _) = findElementWithDataAndOffsetInFile(file, psiClass, marker)
        check(data.isEmpty()) { "Did not expect marker data" }
        return element
    }

    private fun <T: PsiElement> findElementWithDataAndOffsetInFile(
        file: PsiFile,
        psiClass: Class<T>,
        marker: String
    ): Triple<T, String, Int> {
        val elementsWithDataAndOffset = findElementsWithDataAndOffsetInFile(file, psiClass, marker)
        check(elementsWithDataAndOffset.isNotEmpty()) { "No `$marker` marker:\n${file.text}" }
        check(elementsWithDataAndOffset.size <= 1) { "More than one `$marker` marker:\n${file.text}" }
        return elementsWithDataAndOffset.first()
    }

    private fun <T: PsiElement> findElementsWithDataAndOffsetInFile(
        file: PsiFile,
        psiClass: Class<T>,
        marker: String
    ): List<Triple<T, String, Int>> {
        return findElementsWithDataAndOffsetInEditor(
            file,
            file.document!!,
            psiClass,
            marker
        )
    }
}

fun <T: PsiElement> findElementsWithDataAndOffsetInEditor(
    file: PsiFile,
    doc: Document,
    psiClass: Class<T>,
    marker: String
): List<Triple<T, String, Int>> {
    val commentPrefix = LanguageCommenters.INSTANCE.forLanguage(file.language).lineCommentPrefix ?: "//"
    val caretMarker = "$commentPrefix$marker"
    val text = file.text
    val result = mutableListOf<Triple<T, String, Int>>()
    var markerOffset = -caretMarker.length
    while (true) {
        markerOffset = text.indexOf(caretMarker, markerOffset + caretMarker.length)
        if (markerOffset == -1) break
        val data = text.drop(markerOffset).removePrefix(caretMarker).takeWhile { it != '\n' }.trim()
        val markerEndOffset = markerOffset + caretMarker.length - 1
        val markerLine = doc.getLineNumber(markerEndOffset)
        val makerColumn = markerEndOffset - doc.getLineStartOffset(markerLine)
        val elementOffset =
            min(doc.getLineStartOffset(markerLine - 1) + makerColumn, doc.getLineEndOffset(markerLine - 1))
        val elementAtMarker = file.findElementAt(elementOffset)!!

        val element = PsiTreeUtil.getParentOfType(elementAtMarker, psiClass, false)
        if (element != null) {
            result.add(Triple(element, data, elementOffset))
        } else {
            val injectionElement = InjectedLanguageManager.getInstance(file.project)
                .findInjectedElementAt(file, elementOffset)
                ?.let { PsiTreeUtil.getParentOfType(it, psiClass, false) }
                ?: error("No ${psiClass.simpleName} at ${elementAtMarker.text}")
            val injectionOffset = (injectionElement.containingFile.virtualFile as VirtualFileWindow)
                .documentWindow.hostToInjected(elementOffset)
            result.add(Triple(injectionElement, data, injectionOffset))
        }
    }
    return result
}
