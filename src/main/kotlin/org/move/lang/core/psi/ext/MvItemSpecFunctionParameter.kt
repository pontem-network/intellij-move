package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvItemSpecFunctionParameter

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
