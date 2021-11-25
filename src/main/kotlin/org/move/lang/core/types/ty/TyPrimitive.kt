package org.move.lang.core.types.ty

import com.intellij.psi.PsiElement
import org.move.ide.presentation.tyToString
import org.move.lang.core.types.Ability

interface TyPrimitive: Ty {
    override fun abilities() = setOf(Ability.DROP, Ability.COPY, Ability.STORE)
}

object TyBool: TyPrimitive {
    override fun toString(): String = tyToString(this)
}

object TyAddress: TyPrimitive {
    override fun toString(): String = tyToString(this)
}

object TySigner: TyPrimitive {
    override fun abilities() = setOf(Ability.DROP)
    override fun toString(): String = tyToString(this)
}

object TyUnit: TyPrimitive {
    override fun abilities() = Ability.none()
    override fun toString(): String = tyToString(this)
}

class TyInteger(val kind: Kind): TyPrimitive {
    override fun abilities() = Ability.all()

    companion object {
        fun fromName(name: String): TyInteger? =
            Kind.values().find { it.name == name }?.let(::TyInteger)

        fun fromSuffixedLiteral(literal: PsiElement): TyInteger? =
            Kind.values().find { literal.text.endsWith(it.name) }?.let(::TyInteger)

        val DEFAULT_KIND = Kind.NoPrecision
    }

    enum class Kind {
        NoPrecision, u8, u64, u128
    }

    override fun toString(): String = tyToString(this)
}
