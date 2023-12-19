package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.mslLetScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.types.infer.inferReceiverTy
import org.move.lang.core.types.ty.TyStruct
import org.move.stdext.wrapWithList

val MvStructDotField.receiverItem: MvStruct? get() {
    val msl = this.isMsl()
    val dotExpr =
        (this.parent as? MvDotExpr)?.getOriginalOrSelf() ?: return null
    val innerTy = dotExpr.inferReceiverTy(msl)
    if (innerTy !is TyStruct) return null
    val structItem = innerTy.item
    if (!msl) {
        // cannot resolve field if not in the same module as struct definition
        val dotExprModule = dotExpr.namespaceModule ?: return null
        if (structItem.containingModule != dotExprModule) return null
    }
    return structItem
}

class MvStructDotFieldReferenceImpl(
    element: MvStructDotFieldReferenceElement
): MvPolyVariantReferenceCached<MvStructDotFieldReferenceElement>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val dotField = element as MvStructDotField
        val receiverItem = dotField.receiverItem ?: return emptyList()

        val referenceName = dotField.referenceName
        val fields = receiverItem.fields
        for (field in fields) {
            if (field.name == referenceName) {
                return field.wrapWithList()
            }
        }
        return emptyList()
//        return processor.matchAll(itemVis, fields)

//        val itemVis = ItemVis(
//            namespaces,
//            mslLetScope = element.mslLetScope,
//            visibilities = Visibility.local(),
//            itemScope = element.itemScope,
//        )
////        val referenceName = element.referenceName
//        var resolved: MvNamedElement? = null
//        processItems(element, itemVis) {
//            if (it.name == referenceName) {
//                resolved = it.element
//                return@processItems true
//            }
//            return@processItems false
//        }
//        return resolved.wrapWithList()
//        return resolveLocalItem(element, setOf(Namespace.DOT_FIELD))
    }
}

abstract class MvStructDotFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvStructDotField {
    override fun getReference(): MvPolyVariantReference {
        return MvStructDotFieldReferenceImpl(this)
    }
}
