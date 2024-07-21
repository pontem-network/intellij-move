package org.move.utils.tests.resolve

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.move.lang.core.resolve.ref.MvPathReference
import org.move.lang.core.resolve.ref.MvPolyVariantReference

fun checkResolvedFile(
    actualResolveFile: VirtualFile,
    expectedFilePath: String,
    pathResolver: (String) -> VirtualFile?
): TestResolveResult {
    if (expectedFilePath.startsWith("...")) {
        if (!actualResolveFile.path.endsWith(expectedFilePath.drop(3))) {
            return TestResolveResult.Err("Should resolve to $expectedFilePath, was ${actualResolveFile.path} instead")
        }
    } else {
        val expectedResolveFile = pathResolver(expectedFilePath)
            ?: return TestResolveResult.Err("Can't find `$expectedFilePath` file")

        if (actualResolveFile != expectedResolveFile) {
            return TestResolveResult.Err("Should resolve to ${expectedResolveFile.path}, was ${actualResolveFile.path} instead")
        }
    }
    return TestResolveResult.Ok
}

sealed class TestResolveResult {
    object Ok : TestResolveResult()
    data class Err(val message: String) : TestResolveResult()
}

fun PsiElement.findReference(offset: Int): PsiReference? = findReferenceAt(offset - textRange.startOffset)

fun PsiElement.checkedResolve(offset: Int, errorMessagePrefix: String = ""): PsiElement {
    val reference = findReference(offset) ?: error("element doesn't have reference")
    val resolved = reference.resolve() ?: run {
        val multiResolve = (reference as? MvPolyVariantReference)?.multiResolve().orEmpty()
        check(multiResolve.size != 1)
        if (multiResolve.isEmpty()) {
            error("${errorMessagePrefix}Failed to resolve $text")
        } else {
            error("${errorMessagePrefix}Failed to resolve $text, multiple variants:\n${multiResolve.joinToString()}")
        }
    }

    check(reference.isReferenceTo(resolved)) {
        "Incorrect `isReferenceTo` implementation in `${reference.javaClass.name}`"
    }

    return resolved
}
