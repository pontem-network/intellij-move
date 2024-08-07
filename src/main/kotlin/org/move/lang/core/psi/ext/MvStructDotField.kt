package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.processAll
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceBase
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyStruct
import org.move.stdext.wrapWithList

fun processNamedFieldVariants(
    element: MvMethodOrField,
    receiverTy: TyStruct,
    msl: Boolean,
    processor: RsResolveProcessor
): Boolean {
    val structItem = receiverTy.item
    if (!msl) {
        // cannot resolve field if not in the same module as struct definition
        val dotExprModule = element.namespaceModule ?: return false
        if (structItem.containingModule != dotExprModule) return false
    }
    return processor.processAll(structItem.namedFields)
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
