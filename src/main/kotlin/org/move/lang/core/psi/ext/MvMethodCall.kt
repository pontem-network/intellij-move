package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.cli.MoveProject
import org.move.lang.core.completion.getOriginalOrSelf
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

fun MoveProject.getTyItemModule(ty: Ty): MvModule? {
    val norefTy = ty.derefIfNeeded()
    return when (norefTy) {
        is TyVector -> {
            this
                .getModulesFromIndex("vector")
                .firstOrNull {
                    val moduleAddress = it.address(this)?.canonicalValue(this)
                    moduleAddress == "0x00000000000000000000000000000001"
                }
        }
        is TyStruct -> norefTy.item.module
        else -> null
    }
}

fun getMethodVariants(element: MvMethodOrField, receiverTy: Ty, msl: Boolean): MatchSequence<MvFunction> {
    val moveProject = element.moveProject ?: return emptySequence()
    val receiverTyItemModule = moveProject.getTyItemModule(receiverTy) ?: return emptySequence()

    val visibilities = Visibility.publicVisibilitiesFor(element).toMutableSet()
    // structItemModule refers to a module from "before completion", need to return to the .containingModule of the same state
    val elementModule = element.containingModule?.getOriginalOrSelf()
    if (elementModule == receiverTyItemModule) {
        visibilities.add(Visibility.Internal)
    }
    val functions =
        visibilities.flatMap { receiverTyItemModule.visibleFunctions(it) }
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