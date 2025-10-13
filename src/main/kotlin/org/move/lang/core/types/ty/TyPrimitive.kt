package org.move.lang.core.types.ty

import com.intellij.psi.PsiElement
import org.move.ide.presentation.tyToString


abstract class TyPrimitive(val name: String): Ty() {
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

object TyNever: TyPrimitive("()") {
    override fun abilities() = Ability.none()
    override fun toString(): String = "<never>"
}

object TyNum: TyPrimitive("num") {
    override fun abilities() = Ability.all()
    override fun toString(): String = tyToString(this)
}

object TySpecBv: TyPrimitive("bv") {
    override fun abilities() = Ability.all()
    override fun toString(): String = tyToString(this)

}

data class TyInteger(val kind: Kind): TyPrimitive(kind.name.lowercase()) {
    override fun abilities() = Ability.all()

//    fun ulongRange(): ULongRange? {
//        return when (kind) {
//            Kind.u8 -> ULongRange(0u, 255u)
//            Kind.u16 -> ULongRange(0u, 65535u)
//            Kind.u32 -> ULongRange(0u, 4294967295u)
//            Kind.u64 -> ULongRange(0u, 18446744073709551615u)
//            else -> null
//        }
//    }

    fun isDefault(): Boolean = this.kind == DEFAULT_KIND

    companion object {
        fun fromName(name: String): TyInteger =
            Kind.entries.find { it.name == name }?.let(::TyInteger)!!

        fun fromSuffixedLiteral(literal: PsiElement): TyInteger? =
            Kind.entries.find { literal.text.endsWith(it.name) }?.let(::TyInteger)

        val DEFAULT_KIND = Kind.NoPrecision
        val DEFAULT = default()
        val U8 = TyInteger(Kind.u8)

        fun default() = TyInteger(DEFAULT_KIND)
    }

    @Suppress("EnumEntryName")
    enum class Kind {
        NoPrecision, num,
        u8, u16, u32, u64, u128, u256,
        i8, i16, i32, i64, i128, i256
    }

    override fun toString(): String = tyToString(this)
}
