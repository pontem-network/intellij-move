package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ScopeItem
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceBase
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.address
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.*
import org.move.lang.moveProject
import org.move.stdext.wrapWithList

typealias MatchSequence<T> = Sequence<ScopeItem<T>>

fun <T: MvNamedElement> MatchSequence<T>.filterByName(refName: String): Sequence<T> {
    return this
        .filter { it.name == refName }
        .map { it.element }
}

fun Ty.itemModule(moveProject: MoveProject): MvModule? {
    val norefTy = this.derefIfNeeded()
    return when (norefTy) {
        is TyVector -> {
            moveProject
                .getModulesFromIndex("vector")
                .firstOrNull { it.is0x1Address(moveProject) }
        }
        is TyStruct -> norefTy.item.module
        else -> null
    }
}

fun MvModule.is0x1Address(moveProject: MoveProject): Boolean {
    val moduleAddress = this.address(moveProject)?.canonicalValue(moveProject)
    return moduleAddress == "0x00000000000000000000000000000001"
}

fun getMethodVariants(element: MvMethodOrField, receiverTy: Ty, msl: Boolean): MatchSequence<MvFunction> {
    val moveProject = element.moveProject ?: return emptySequence()
    val receiverTyItemModule = receiverTy.itemModule(moveProject) ?: return emptySequence()

    val elementScopes = Visibility.visibilityScopesForElement(element).toMutableSet()
    if (element.containingModule == receiverTyItemModule) {
        elementScopes.add(Visibility.Internal)
    }
    val functions =
        elementScopes
            .flatMap { elementScope -> receiverTyItemModule.functionsVisibleInScope(elementScope) }
            .filter {
                val selfTy = it.selfParamTy(msl) ?: return@filter false
                // need to use TyVar here, loweredType() erases them
                val selfTyWithTyVars =
                    selfTy.foldTyTypeParameterWith { tp -> TyInfer.TyVar(tp) }
                TyReference.isCompatibleWithAutoborrow(receiverTy, selfTyWithTyVars, msl)
            }
    return functions
        .filter { it.name != null }
        .map { ScopeItem(it.name!!, it) }.asSequence()
}

class MvMethodCallReferenceImpl(
    element: MvMethodCall
):
    MvPolyVariantReferenceBase<MvMethodCall>(element) {

    override fun multiResolve(): List<MvNamedElement> {
        val msl = element.isMsl()
        val receiverExpr = element.receiverExpr
        val inference = receiverExpr.inference(msl) ?: return emptyList()
        return inference.getResolvedMethod(element).wrapWithList()
    }

    override fun isReferenceTo(element: PsiElement): Boolean =
        element is MvFunction && super.isReferenceTo(element)
}

abstract class MvMethodCallMixin(node: ASTNode): MvElementImpl(node),
                                                 MvMethodCall {

    override fun getReference(): MvPolyVariantReference = MvMethodCallReferenceImpl(this)
}