package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceBase
import org.move.lang.core.resolve2.ref.MvMethodCallReferenceImpl
import org.move.lang.core.types.address
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyVector
import org.move.stdext.wrapWithList

fun Ty.itemModule(moveProject: MoveProject): MvModule? {
    val norefTy = this.derefIfNeeded()
    return when (norefTy) {
        is TyVector -> {
            moveProject
                .getModulesFromIndex("vector")
                .firstOrNull { it.is0x1Address(moveProject) }
        }
        is TyAdt -> norefTy.item.module
        else -> null
    }
}

fun MvModule.is0x1Address(moveProject: MoveProject): Boolean = this.address(moveProject)?.is0x1 ?: false

abstract class MvMethodCallMixin(node: ASTNode): MvElementImpl(node), MvMethodCall {

    override fun getReference(): MvPolyVariantReference = MvMethodCallReferenceImpl(this)
}

