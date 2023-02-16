package org.move.lang.core.psi.ext

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.*
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
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

val IS_MSL_KEY: Key<CachedValue<Boolean>> = Key.create("org.move.cache.IS_MSL_KEY")

fun MvElement.isMsl(): Boolean {
    // use items always non-msl, otherwise import resolution doesn't work correctly
    if (this is MvUseItem) return false

    val context = this
    return CachedValuesManager.getCachedValue(context, IS_MSL_KEY) {
        var element: MvElement? = context
        var isMsl = false
        while (element != null) {
            if (element is MslScopeElement) {
                isMsl = true
                break
            }
            element = element.parent as? MvElement
        }
        CachedValueProvider.Result.create(isMsl, PsiModificationTracker.MODIFICATION_COUNT)
    }
}
