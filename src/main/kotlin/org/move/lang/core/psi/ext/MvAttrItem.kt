package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNamedElementImpl
import org.move.lang.core.resolve.ref.MvPath2Reference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceBase
import org.move.lang.core.resolve.PathKind
import org.move.lang.core.resolve.pathKind

val MvAttrItem.unqualifiedIdent: PsiElement?
    get() {
        val path = this.path
        if (path.pathKind(false) !is PathKind.UnqualifiedPath) return null
        return path.identifier
    }
val MvAttrItem.unqualifiedName: String? get() = this.unqualifiedIdent?.text

val MvAttrItem.attr: MvAttr? get() = this.parent as? MvAttr
val MvAttrItem.innerAttrItems: List<MvAttrItem> get() = this.attrItemList?.attrItemList.orEmpty()

val MvAttrItem.isAbortCode: Boolean get() = this.unqualifiedIdent?.textMatches("abort_code") == true
val MvAttrItem.isTest: Boolean get() = this.unqualifiedIdent?.textMatches("test") == true

class AttrItemReferenceImpl(
    element: MvPath,
    val ownerFunction: MvFunction
) : MvPolyVariantReferenceBase<MvPath>(element), MvPath2Reference {

    override fun multiResolve(): List<MvNamedElement> {
        return ownerFunction.parameters
            .map { it.patBinding }
            .filter { it.name == element.referenceName }
    }

//    override fun multiResolveInner(): List<MvNamedElement> {
//    }
}

abstract class MvAttrItemMixin(node: ASTNode): MvNamedElementImpl(node),
                                               MvAttrItem {

//    override fun getReference(): MvPolyVariantReference? {
//        val attr = this.ancestorStrict<MvAttr>() ?: return null
//        attr.attrItemList
//            .singleOrNull()
//            ?.takeIf { it.isTest } ?: return null
//        val ownerFunction = attr.attributeOwner as? MvFunction ?: return null
//        return AttrItemReferenceImpl(this, ownerFunction)
//    }

}
