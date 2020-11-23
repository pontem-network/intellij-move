package org.move.utils.tests.resolve

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.utils.tests.MoveTestCase
import org.move.utils.tests.fileTreeFromText
import java.nio.file.Path
import java.nio.file.Paths

abstract class ResolveTestCase : MoveTestCase() {
    protected fun stubOnlyResolve(@Language("Move") code: String) {
        val fileTree = fileTreeFromText(code)
        val testProject = fileTree.createAndOpenFileWithCaretMarker()

        checkAstNotLoaded { file ->
            !file.path.endsWith(testProject.fileWithCaret)
        }

        val (reference, resolveFile, offset) = findElementWithDataAndOffsetInEditor<MoveReferenceElement>()

        if (resolveFile == "unresolved") {
            val element = reference.reference.resolve()
            if (element != null) {
                error("Should not resolve ${reference.text} to ${element.text}")
            }
            return
        }

        val element = reference.checkedResolve(offset)
        val actualResolveFile = element.containingFile.virtualFile

        if (resolveFile.startsWith("...")) {
            check(actualResolveFile.path.endsWith(resolveFile.drop(3))) {
                "Should resolve to $resolveFile, was ${actualResolveFile.path} instead"
            }
        } else {
            val expectedResolveFile = myFixture.findFileInTempDir(resolveFile)
                ?: error("Can't find `$resolveFile` file")

            check(actualResolveFile == expectedResolveFile) {
                "Should resolve to ${expectedResolveFile.path}, was ${actualResolveFile.path} instead"
            }
        }
    }

    protected fun checkByFileTree(@Language("Move") code: String) {
        val fileTree = fileTreeFromText(code)
        val vfm = VirtualFileManager.getInstance()
        val rootDirectory =
            vfm.findFileByNioPath(Paths.get(myFixture.project.basePath!!))!!
        if (rootDirectory.children.isNotEmpty()) {
            rootDirectory.children.forEach {  }
        }
        fileTree.create(myFixture.project, rootDirectory)

        val filePath = rootDirectory.toNioPath().resolve("main.move")
        val file = vfm.findFileByNioPath(filePath)!!
        myFixture.configureFromExistingVirtualFile(file)

        val (refElement, data, offset) = findElementWithDataAndOffsetInEditor<MoveReferenceElement>("^")

        if (data == "unresolved") {
            val resolved = refElement.reference.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }

        val resolved = refElement.checkedResolve(offset)

        val target = findElementInEditor(MoveNamedElement::class.java, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }

//    protected fun checkByCode(
//        @Language("Move") code: String,
//    ) = checkByCodeGeneric(MoveNamedElement::class.java, code)

    protected fun checkByCode(
        @Language("Move") code: String,
    ) {
        InlineFile(code, "main.move")
//        val modifiedCode = "//- main.move\n$code"
//        val fileTree = fileTreeFromText(modifiedCode)
//        val vfm = VirtualFileManager.getInstance()
//        val rootDirectory =
//            vfm.findFileByNioPath(Path.of(myFixture.project.basePath))!!
//        fileTree.create(myFixture.project, rootDirectory)
//
//        val filePath = rootDirectory.toNioPath().resolve("main.move")
//        val file = vfm.findFileByNioPath(filePath)!!
//        myFixture.configureFromExistingVirtualFile(file)
////        myFixture.configureFromTempProjectFile(testProject.fileWithCaret)

        val (refElement, data, offset) = findElementWithDataAndOffsetInEditor<MoveReferenceElement>("^")

        if (data == "unresolved") {
            val resolved = refElement.reference.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }

        val resolved = refElement.checkedResolve(offset)

        val target = findElementInEditor(MoveNamedElement::class.java, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }
}

fun PsiElement.findReference(offset: Int): PsiReference? = findReferenceAt(offset - textRange.startOffset)

fun PsiElement.checkedResolve(offset: Int): PsiElement {
    val reference = findReference(offset) ?: error("element doesn't have reference")
    val resolved = reference.resolve() ?: error("Failed to resolve `$text`")

    check(reference.isReferenceTo(resolved)) {
        "Incorrect `isReferenceTo` implementation in `${reference.javaClass.name}`"
    }

    return resolved
}