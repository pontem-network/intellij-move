package org.move.lang.core.psi.ext

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.*
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.FolderScope
import org.move.lang.core.resolve.ItemScope
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull

private val IS_MSL_KEY: Key<CachedValue<Boolean>> = Key.create("IS_MSL_KEY")

fun PsiElement.isMsl(): Boolean {
    if (this !is MvElement) return false
    return CachedValuesManager.getCachedValue(this, IS_MSL_KEY) {
        val specElement = PsiTreeUtil.findFirstParent(this, false) {
            it is MvSpecFunction
                    || it is MvSpecBlockExpr
                    || it is MvSchema
                    || it is MvAnySpec
        }
        CachedValueProvider.Result(specElement != null, PsiModificationTracker.MODIFICATION_COUNT)
    }
}

fun PsiElement.cameBefore(element: PsiElement) =
    PsiUtilCore.compareElementsByPosition(this, element) <= 0

fun MvElement.isInsideAssignmentLeft(): Boolean {
    val parent = PsiTreeUtil.findFirstParent(this, false) {
        it is MvAssignmentExpr || it is MvInitializer
    }
    return parent is MvAssignmentExpr
}

fun PsiFileSystemItem.findMoveProject(): MoveProject? {
    if (this is MoveFile) return this.moveProject
    val path = virtualFile.toNioPathOrNull() ?: return null
    return project.moveProjects.findMoveProject(path)
}

val MvElement.itemScope: ItemScope
    get() {
        val testOnly = ancestors
            .filterIsInstance<MvDocAndAttributeOwner>()
            .any { it.isTestOnly }
        return if (testOnly) ItemScope.TEST_ONLY else ItemScope.MAIN
    }

val MvElement.folderScope: FolderScope
    get() {
        val testsFolderPath = this.moveProject?.contentRoot
            ?.findChild("tests")
            ?.takeIf { it.exists() }
            ?.path ?: return FolderScope.SOURCES
        val isTestFolder =
            this.containingFile?.originalFile?.virtualFile?.path?.startsWith(testsFolderPath) ?: false
//        val isTestFolder = this.containingFile?.parents(false)
//            ?.any {
//                it is PsiDirectory
//                        && it.virtualFile.path == testsFolderPath
//            } ?: false
        return if (isTestFolder) FolderScope.TESTS else FolderScope.SOURCES
    }
