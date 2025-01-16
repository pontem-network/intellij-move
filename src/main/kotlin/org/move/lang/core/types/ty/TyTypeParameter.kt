package org.move.lang.core.types.ty

import com.intellij.codeInsight.completion.CompletionUtil
import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.ext.ability
import org.move.lang.core.psi.ext.abilityBounds
import org.move.lang.core.psi.ext.requiredAbilitiesForTypeParam
import org.move.lang.core.types.infer.HAS_TY_TYPE_PARAMETER_MASK


class TyTypeParameter private constructor(
    val origin: MvTypeParameter
) : Ty(HAS_TY_TYPE_PARAMETER_MASK) {

    val name: String? get() = origin.name

    override fun abilities(): Set<Ability> {
        val paramAbilities = origin.abilityBounds.mapNotNull { it.ability }
        if (paramAbilities.isNotEmpty()) {
            return paramAbilities.toSet()
        }
        val parentStruct = origin.parent.parent as? MvStruct
        return parentStruct?.requiredAbilitiesForTypeParam.orEmpty()
    }

    override fun equals(other: Any?): Boolean = other is TyTypeParameter && other.origin == origin
    override fun hashCode(): Int = origin.hashCode()

    override fun toString(): String = tyToString(this)

    companion object {
        fun named(parameter: MvTypeParameter): TyTypeParameter {
            // Treat the same parameters from original/copy files as equals
            val originalParameter = CompletionUtil.getOriginalOrSelf(parameter)
            return TyTypeParameter(originalParameter)
        }
    }
}
