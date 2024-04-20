package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ScopeItem
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.*

typealias MatchSequence<T> = Sequence<ScopeItem<T>>

fun <T: MvNamedElement> MatchSequence<T>.filterByName(refName: String): Sequence<T> {
    return this
        .filter { it.name == refName }
        .map { it.element }
}

fun getMethodVariants(element: MvMethodOrField, receiverTy: Ty, msl: Boolean): MatchSequence<MvFunction> {
    // TODO: vector
    if (receiverTy is TyVector) return emptySequence()

    val structItem = (receiverTy.derefIfNeeded() as? TyStruct)?.item ?: return emptySequence()

    val structItemModule = structItem.module
    val visibilities = Visibility.publicVisibilitiesFor(element).toMutableSet()
    // structItemModule refers to a module from "before completion", need to return to the .containingModule of the same state
    val elementModule = element.containingModule?.getOriginalOrSelf()
    if (elementModule == structItemModule) {
        visibilities.add(Visibility.Internal)
    }
    val functions =
        visibilities.flatMap { structItemModule.visibleFunctions(it) }
            .filter {
                val selfParam = it.selfParameter ?: return@filter false
                // TODO: support vector
                val selfTy = selfParam.type?.loweredType(msl) ?: return@filter false
                TyReference.isCompatibleWithAutoborrow(receiverTy, selfTy)
            }
    return functions
        .filter { it.name != null }
        .map { ScopeItem(it.name!!, it) }.asSequence()
}

class MvMethodCallReferenceImpl(
    element: MvMethodCall
):
    MvPolyVariantReferenceCached<MvMethodCall>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val msl = element.isMsl()
        val refName = element.referenceName

        val receiverTy = element.inferReceiverTy(msl).knownOrNull() ?: return emptyList()
        val methods = getMethodVariants(element, receiverTy, msl)
        return methods
            .filterByName(refName).toList()
    }

    override fun isReferenceTo(element: PsiElement): Boolean =
        element is MvFunction && super.isReferenceTo(element)
}

abstract class MvMethodCallMixin(node: ASTNode): MvElementImpl(node),
                                                 MvMethodCall {

    override fun getReference(): MvPolyVariantReference = MvMethodCallReferenceImpl(this)
}