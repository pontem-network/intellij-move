package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvItemSpecParameterReferenceElement
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached

val MvItemSpecFunctionParameter.parameterList get() = this.parent as? MvItemSpecFunctionParameterList

val MvItemSpecFunctionParameter.itemSpec: MvItemSpec?
    get() =
        parameterList?.parent?.parent as? MvItemSpec

val MvItemSpecTypeParameter.parameterList get() = this.parent as? MvItemSpecTypeParameterList

val MvItemSpecTypeParameter.itemSpec: MvItemSpec?
    get() =
        parameterList?.parent?.parent as? MvItemSpec

val MvItemSpecTypeParameter.bounds: List<MvAbility>
    get() =
        typeParamBound?.abilityList.orEmpty()

class MvItemSpecParameterReferenceImpl(
    element: MvItemSpecParameterReferenceElement,
) : MvPolyVariantReferenceCached<MvItemSpecParameterReferenceElement>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val element = this.element
        when (element) {
            is MvItemSpecFunctionParameter -> {
                val funcItem = element.itemSpec?.funcItem
                if (funcItem != null) {
                    val refName = element.referenceName
                    for (bindingPat in funcItem.parametersAsBindings) {
                        if (bindingPat.name == refName) {
                            return listOf(bindingPat)
                        }
                    }
                }
            }
            is MvItemSpecTypeParameter -> {
                val funcItem = element.itemSpec?.funcItem
                if (funcItem != null) {
                    val refName = element.referenceName
                    for (typeParameter in funcItem.typeParameters) {
                        if (typeParameter.name == refName) {
                            return listOf(typeParameter)
                        }
                    }
                }
            }
        }
        return emptyList()
    }
}

abstract class MvItemSpecFunctionParameterMixin(node: ASTNode) : MvElementImpl(node),
                                                                 MvItemSpecFunctionParameter {
    override fun getReference(): MvPolyVariantReference {
        return MvItemSpecParameterReferenceImpl(this)
    }
}

abstract class MvItemSpecTypeParameterMixin(node: ASTNode) : MvElementImpl(node),
                                                             MvItemSpecTypeParameter {
    override fun getReference(): MvPolyVariantReference {
        return MvItemSpecParameterReferenceImpl(this)
    }
}
