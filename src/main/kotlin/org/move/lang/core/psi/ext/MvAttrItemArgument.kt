package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvReferenceCached

class AttrItemArgumentReferenceImpl(
    element: MvAttrItemArgument,
    val ownerFunction: MvFunction
) : MvReferenceCached<MvAttrItemArgument>(element) {

    override fun resolveInner(): List<MvNamedElement> {
        return ownerFunction.parameterBindings
            .filter { it.name == element.referenceName }
    }
}

abstract class MvAttrItemArgumentMixin(node: ASTNode) : MvElementImpl(node),
                                                        MvAttrItemArgument {

    override fun getReference(): MvReference? {
        val attr = this.ancestorStrict<MvAttr>() ?: return null
        attr.attrItemList
            .singleOrNull()
            ?.takeIf { it.identifier.text == "test" } ?: return null
        val ownerFunction = attr.owner as? MvFunction ?: return null
        return AttrItemArgumentReferenceImpl(this, ownerFunction)
    }
}
