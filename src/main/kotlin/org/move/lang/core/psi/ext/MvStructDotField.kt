package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.types.ty.TyStruct

class MvStructDotFieldReferenceImpl(
    element: MvStructDotField
): MvPolyVariantReferenceCached<MvStructDotField>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val msl = element.isMsl()
        val receiverTy = element.inferReceiverTy(msl).derefIfNeeded()
        if (receiverTy !is TyStruct) return emptyList()

        val structItem = receiverTy.item
        if (!msl) {
            // cannot resolve field if not in the same module as struct definition
            val dotExprModule = element.namespaceModule ?: return emptyList()
            if (structItem.containingModule != dotExprModule) return emptyList()
        }

        val referenceName = element.referenceName
        return structItem.fields
            .filter { it.name == referenceName }
    }
}

abstract class MvStructDotFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvStructDotField {
    override fun getReference(): MvPolyVariantReference {
        return MvStructDotFieldReferenceImpl(this)
    }
}
