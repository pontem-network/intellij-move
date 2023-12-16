package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached

class AttrItemArgumentReferenceImpl(
    element: MvAttrItemArgument,
    val ownerFunction: MvFunction
) : MvPolyVariantReferenceCached<MvAttrItemArgument>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        return ownerFunction.parameters
            .map { it.bindingPat }
            .filter { it.name == element.referenceName }
    }
}

abstract class MvAttrItemArgumentMixin(node: ASTNode) : MvElementImpl(node),
                                                        MvAttrItemArgument {

    override fun getReference(): MvPolyVariantReference? {
        val attr = this.ancestorStrict<MvAttr>() ?: return null
        attr.attrItemList
            .singleOrNull()
            ?.takeIf { it.identifier.text == "test" } ?: return null
        val ownerFunction = attr.owner as? MvFunction ?: return null
        return AttrItemArgumentReferenceImpl(this, ownerFunction)
    }
}
