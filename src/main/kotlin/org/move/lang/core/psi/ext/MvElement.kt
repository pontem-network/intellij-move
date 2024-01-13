package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull

fun MvElement.isInsideAssignmentLhs(): Boolean {
    val parent = PsiTreeUtil.findFirstParent(this, false) {
        it is MvAssignmentExpr || it is MvInitializer
    }
    return parent is MvAssignmentExpr
}

fun PsiFileSystemItem.findMoveProject(): MoveProject? {
    if (this is MoveFile) return this.moveProject
    val path = virtualFile.toNioPathOrNull() ?: return null
    return project.moveProjectsService.findMoveProject(path)
}

//private val MSL_KEY: Key<CachedValue<FunctionSignature?>> = Key.create("SIGNATURE_KEY")

val MvNamedElement.isMslOnlyItem: Boolean
    get() {
        var element: PsiElement? = this
        while (element != null) {
            // use items always non-msl, otherwise import resolution doesn't work correctly
            if (element is MvUseItem) return false

            // module items
            if (element is MvModule
                || element is MvFunction
                || element is MvStruct
            )
                return false

            if (element is MslOnlyElement) return true

            element = element.parent as? MvElement
        }
        return false
    }

val MvPath.isMslScope: Boolean get() = this.isMslInner()

val MvModuleRef.isMslScope: Boolean get() = this.isMslInner()

@Deprecated("Use specialized receiver type property isMslScope if possible")
fun PsiElement.isMsl(): Boolean = isMslInner()
//fun PsiElement.isMslLegacy(): Boolean {
//    return CachedValuesManager.getProjectPsiDependentCache(this) {
//        var element: PsiElement? = it
//        while (element != null) {
//            // use items always non-msl, otherwise import resolution doesn't work correctly
//            if (element is MvUseItem) return@getProjectPsiDependentCache false
//
//            // module items
//            if (element is MvModule
//                || element is MvFunction
//                || element is MvStruct
//            )
//                return@getProjectPsiDependentCache false
//
//            if (element is MslOnlyElement) return@getProjectPsiDependentCache true
//
//            element = element.parent as? MvElement
//        }
//        false
//    }
//}

private fun PsiElement.isMslInner(): Boolean {
    return CachedValuesManager.getProjectPsiDependentCache(this) {
        var element: PsiElement? = it
        while (element != null) {
            // use items always non-msl, otherwise import resolution doesn't work correctly
            if (element is MvUseItem) return@getProjectPsiDependentCache false

            // module items
            if (element is MvModule
                || element is MvFunction
                || element is MvStruct
            )
                return@getProjectPsiDependentCache false

            if (element is MslOnlyElement) return@getProjectPsiDependentCache true

            element = element.parent as? MvElement
        }
        false
    }
}
