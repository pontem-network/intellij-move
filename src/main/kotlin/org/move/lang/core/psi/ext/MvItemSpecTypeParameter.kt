package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvItemSpecFunctionParameter
import org.move.lang.core.psi.MvItemSpecTypeParameter
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveSingleItem

//class MvItemSpecTypeParameterReferenceImpl(
//    element: MvItemSpecTypeParameter
//) : MvReferenceCached<MvItemSpecTypeParameter>(element) {
//
//    override fun resolveInner(): List<MvNamedElement> {
//        return emptyList()
//    }
//}

abstract class MvItemSpecTypeParameterMixin(node: ASTNode) : MvElementImpl(node),
                                                             MvItemSpecTypeParameter {
//    override fun getReference(): MvReference {
//        return MvItemSpecTypeParameterReferenceImpl(this)
//    }
}
