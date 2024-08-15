package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.processAll
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceBase
import org.move.lang.core.resolve.ref.NONE
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyAdt
import org.move.stdext.wrapWithList

fun processNamedFieldVariants(
    element: MvMethodOrField,
    receiverTy: TyAdt,
    msl: Boolean,
    processor: RsResolveProcessor
): Boolean {
    val receiverItem = receiverTy.item
    if (!isFieldsAccessible(element, receiverItem, msl)) return false

    return when (receiverItem) {
        is MvStruct -> processor.processAll(NONE, receiverItem.namedFields)
        is MvEnum -> {
            var found = false
            for (variant in receiverItem.variants) {
                if (!found) {
                    found = processor.processAll(NONE, variant.namedFields)
                }
            }
            found
        }
        else -> error("unreachable")
    }
}

// todo: change into VisibilityFilter
private fun isFieldsAccessible(
    element: MvElement,
    item: MvStructOrEnumItemElement,
    msl: Boolean
): Boolean {
    if (!msl) {
        // cannot resolve field if not in the same module as struct definition
        val dotExprModule = element.namespaceModule ?: return false
        if (item.containingModule != dotExprModule) return false
    }
    return true
}

class MvStructDotFieldReferenceImpl(
    element: MvStructDotField
): MvPolyVariantReferenceBase<MvStructDotField>(element) {

    override fun multiResolve(): List<MvNamedElement> {
        val msl = element.isMsl()
        val receiverExpr = element.receiverExpr
        val inference = receiverExpr.inference(msl) ?: return emptyList()
        return inference.getResolvedField(element).wrapWithList()
    }
}

abstract class MvStructDotFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvStructDotField {
    override fun getReference(): MvPolyVariantReference {
        return MvStructDotFieldReferenceImpl(this)
    }
}
