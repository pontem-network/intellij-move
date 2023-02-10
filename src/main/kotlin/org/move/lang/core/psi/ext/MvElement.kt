package org.move.lang.core.psi.ext

import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvAssignmentExpr
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvInitializer
import org.move.lang.core.resolve.ItemScope
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull

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
        return getProjectPsiDependentCache(this) {
            for (ancestor in (sequenceOf(it) + it.ancestors)) {
                when {
                    ancestor is MvFunction && ancestor.isTest ->
                        return@getProjectPsiDependentCache ItemScope.TEST
                    ancestor is MvDocAndAttributeOwner && ancestor.isTestOnly ->
                        return@getProjectPsiDependentCache ItemScope.TEST
                }
            }
            ItemScope.MAIN
        }
    }
