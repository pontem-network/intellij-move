package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvAbility
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

val MvTypeParameter.isPhantom get() = hasChild(MvElementTypes.PHANTOM)

val MvTypeParameter.abilityBounds: List<MvAbility>
    get() {
        return typeParamBound?.abilityList.orEmpty()
    }

abstract class MvTypeParameterMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                                    MvTypeParameter