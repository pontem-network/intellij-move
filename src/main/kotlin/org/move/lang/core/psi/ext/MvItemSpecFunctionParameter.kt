package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvItemSpecFunctionParameter
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveSingleItem

//class MvItemSpecFunctionParameterReferenceImpl(
//    element: MvItemSpecFunctionParameter
//) : MvReferenceCached<MvItemSpecFunctionParameter>(element) {
//
//    override fun resolveInner(): List<MvNamedElement> {
//        return listOfNotNull(
//            resolveSingleItem(element, setOf(Namespace.FUNCTION_PARAM))
//        )
//
//    }
//}

abstract class MvItemSpecFunctionParameterMixin(node: ASTNode) : MvElementImpl(node),
                                                                 MvItemSpecFunctionParameter {
//    override fun getReference(): MvReference {
//        return MvItemSpecFunctionParameterReferenceImpl(this)
//    }
}
