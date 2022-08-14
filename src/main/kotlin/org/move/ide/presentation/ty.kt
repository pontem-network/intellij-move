package org.move.ide.presentation

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingModule
import org.move.lang.core.types.ty.*

fun Ty.acquireableIn(mod: MvModule): Boolean {
    // no declaring module means builtin
    val declaringMod = this.declaringModule ?: return false
    return declaringMod == mod
}

private val Ty.declaringModule: MvModule?
    get() = when (this) {
        is TyReference -> this.referenced.declaringModule
        is TyStruct -> this.item.containingModule
        else -> null
    }

fun Ty.nameNoArgs(): String {
    return this.name().replace(Regex("<.*>"), "")
}

fun Ty.name(): String {
    return text(fq = false)
}

fun Ty.fullnameNoArgs(): String {
    return this.fullname().replace(Regex("<.*>"), "")
}

fun Ty.fullname(): String {
    return text(fq = true)
}

fun Ty.typeLabel(relativeTo: MvElement): String {
    val typeModule = this.declaringModule
    if (typeModule != null && typeModule != relativeTo.containingModule) {
        return this.fullname()
    } else {
        return this.name()
    }
}

fun Ty.text(fq: Boolean = false): String =
    render(
        this,
        level = 3,
        fq = fq
    )

val Ty.insertionSafeText: String
    get() = render(
        this,
        level = Int.MAX_VALUE,
        unknown = "_",
        anonymous = "_",
        integer = "_"
    )

fun tyToString(ty: Ty) = render(ty, Int.MAX_VALUE)

private fun render(
    ty: Ty,
    level: Int,
    unknown: String = "<unknown>",
    anonymous: String = "<anonymous>",
    integer: String = "integer",
    fq: Boolean = false
): String {
    check(level >= 0)
    if (ty is TyUnknown) return unknown
    if (ty is TyPrimitive) {
        return when (ty) {
            is TyBool -> "bool"
            is TyAddress -> "address"
            is TySigner -> "signer"
            is TyUnit -> "()"
            is TyNum -> "num"
            is TyInteger -> {
                if (ty.kind == TyInteger.DEFAULT_KIND) {
                    integer
                } else {
                    ty.kind.toString()
                }
            }
            is TyNever -> "<never>"
            else -> error("unreachable")
        }
    }

    if (level == 0) return "_"

    val r = { subTy: Ty -> render(subTy, level - 1, unknown, anonymous, integer, fq) }

    return when (ty) {
        is TyFunction -> {
            val params = ty.paramTypes.joinToString(", ", "fn(", ")", transform = r)
            var s = if (ty.retType is TyUnit) params else "$params -> ${ty.retType}"
            if (ty.acquiresTypes.isNotEmpty()) {
                s += ty.acquiresTypes.joinToString(", ", " acquires ", transform = r)
            }
            s
        }
        is TyTuple -> ty.types.joinToString(", ", "(", ")", transform = r)
        is TyVector -> "vector<${render(ty.item, level, unknown, anonymous, integer, fq)}>"
        is TyReference -> "${if (ty.permissions.contains(RefPermissions.WRITE)) "&mut " else "&"}${
            render(ty.referenced, level, unknown, anonymous, integer, fq)
        }"
//        is TyTraitObject -> ty.trait.name ?: anonymous
        is TyTypeParameter -> ty.name ?: anonymous
        is TyStruct -> {
            val name = if (fq) ty.item.fqName else (ty.item.name ?: anonymous)
            val args =
                if (ty.typeArgs.isEmpty()) ""
                else ty.typeArgs.joinToString(", ", "<", ">", transform = r)
            name + args
        }
        is TyInfer -> when (ty) {
            is TyInfer.TyVar -> "?${ty.origin?.name ?: "_"}"
            is TyInfer.IntVar -> integer
        }
        else -> error("unreachable")
    }
}
