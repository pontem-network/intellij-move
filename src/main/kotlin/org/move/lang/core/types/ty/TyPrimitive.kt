package org.move.lang.core.types.ty

import com.intellij.psi.PsiElement
import org.move.ide.presentation.tyToString


abstract class TyPrimitive(val name: String): Ty {
    override fun abilities() = setOf(Ability.DROP, Ability.COPY, Ability.STORE)
}

object TyBool: TyPrimitive("bool") {
    override fun toString(): String = tyToString(this)
}

object TyAddress: TyPrimitive("address") {
    override fun toString(): String = tyToString(this)
}

object TySigner: TyPrimitive("signer") {
    override fun abilities() = setOf(Ability.DROP)
    override fun toString(): String = tyToString(this)
}

object TyUnit: TyPrimitive("()") {
    override fun abilities() = Ability.none()
    override fun toString(): String = tyToString(this)
}

object TyNum: TyPrimitive("num") {
    override fun abilities() = Ability.all()
    override fun toString(): String = tyToString(this)
}

data class TyInteger(val kind: Kind): TyPrimitive(kind.name.lowercase()) {
    override fun abilities() = Ability.all()

    companion object {
        fun fromName(name: String): TyInteger =
            Kind.values().find { it.name == name }?.let(::TyInteger)!!

        fun fromSuffixedLiteral(literal: PsiElement): TyInteger? =
            Kind.values().find { literal.text.endsWith(it.name) }?.let(::TyInteger)

        val DEFAULT_KIND = Kind.NoPrecision
    }

    enum class Kind {
        NoPrecision, u8, u64, u128, num
    }

    override fun toString(): String = tyToString(this)
}
