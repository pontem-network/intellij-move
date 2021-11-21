package org.move.lang.core.types.ty

import com.intellij.psi.PsiElement
import org.move.ide.presentation.tyToString

interface TyPrimitive: Ty

object TyBool: TyPrimitive {
    override fun toString(): String = tyToString(this)
}

object TyAddress: TyPrimitive {
    override fun toString(): String = tyToString(this)
}

object TySigner: TyPrimitive {
    override fun toString(): String = tyToString(this)
}

object TyUnit: TyPrimitive {
    override fun toString(): String = tyToString(this)
}

class TyInteger(val kind: Kind): TyPrimitive {
    companion object {
        fun fromName(name: String): TyInteger? =
            Kind.values().find { it.name == name }?.let(::TyInteger)

        fun fromSuffixedLiteral(literal: PsiElement): TyInteger? =
            Kind.values().find { literal.text.endsWith(it.name) }?.let(::TyInteger)

        val DEFAULT_KIND = Kind.u64
    }

    enum class Kind {
        u8, u64, u128
    }

    override fun toString(): String = tyToString(this)
}
