package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNamedElementImpl
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached

val MvAttrItem.attr: MvAttr? get() = this.parent as? MvAttr
val MvAttrItem.innerAttrItems: List<MvAttrItem> get() = this.attrItemList?.attrItemList.orEmpty()

val MvAttrItem.isAbortCode: Boolean get() = this.identifier.textMatches("abort_code")

class AttrItemReferenceImpl(
    element: MvAttrItem,
    val ownerFunction: MvFunction
) : MvPolyVariantReferenceCached<MvAttrItem>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        return ownerFunction.parameters
            .map { it.patBinding }
            .filter { it.name == element.referenceName }
    }
}

abstract class MvAttrItemMixin(node: ASTNode): MvNamedElementImpl(node),
                                               MvAttrItem {

    override fun getReference(): MvPolyVariantReference? {
        val attr = this.ancestorStrict<MvAttr>() ?: return null
        attr.attrItemList
            .singleOrNull()
            ?.takeIf { it.identifier.text == "test" } ?: return null
        val ownerFunction = attr.owner as? MvFunction ?: return null
        return AttrItemReferenceImpl(this, ownerFunction)
    }

}
