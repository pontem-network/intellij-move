package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.MatchingProcessor
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown

fun processMethods(element: MvMethodCall, msl: Boolean, processor: MatchingProcessor<MvFunction>) {
    val inference = element.inference(msl) ?: return
    val receiverTy = inference.getExprType(element.receiverExpr).takeIf { it !is TyUnknown }
        ?: return

    // TODO: vector
    val structItem = (receiverTy.derefIfNeeded() as? TyStruct)?.item ?: return

    val structItemModule = structItem.module
    val visibilities = Visibility.publicVisibilitiesFor(element).toMutableSet()
    if (element.containingModule == structItemModule) {
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
    processor.matchAll(functions)
}

class MvMethodCallReferenceImpl(
    element: MvMethodCall
):
    MvPolyVariantReferenceCached<MvMethodCall>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val msl = element.isMsl()
        val refName = element.referenceName
        val resolved = mutableListOf<MvNamedElement>()
        processMethods(element, msl) {
            if (it.name == refName) {
                resolved.add(it.element)
            }
            false
        }
        return resolved
//        val inference = element.inference(msl) ?: return emptyList()
//        val receiverTy = inference.getExprType(element.receiverExpr).takeIf { it !is TyUnknown }
//            ?: return emptyList()
//
//        // TODO: vector
//        val structItem = (receiverTy.derefIfNeeded() as? TyStruct)?.item ?: return emptyList()
//
//        val structItemModule = receiverTy.declaringModule ?: return emptyList()
//        val visibilities = Visibility.buildSetOfVisibilities(element)
//        val refName = element.referenceName
//
//        val functions = visibilities.flatMap { structItemModule.visibleFunctions(it) }
    }

    override fun isReferenceTo(element: PsiElement): Boolean =
        element is MvFunctionLike && super.isReferenceTo(element)
}

abstract class MvMethodCallMixin(node: ASTNode): MvElementImpl(node),
                                                 MvMethodCall {

    override fun getReference(): MvPolyVariantReference = MvMethodCallReferenceImpl(this)
}