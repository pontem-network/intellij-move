package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvItemSpecTypeParameter

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
