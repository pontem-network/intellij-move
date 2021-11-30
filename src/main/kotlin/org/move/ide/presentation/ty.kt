package org.move.ide.presentation

import org.move.lang.MoveFile
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.ext.fqName
import org.move.lang.core.types.ty.*

fun Ty.getDefiningModule(): MoveModuleDef? =
    when (this) {
        is TyReference -> this.referenced.getDefiningModule()
        is TyStruct -> this.item.containingModule
        else -> null
    }

fun Ty.name(): String {
    return shortPresentableText(fq = false)
}

fun Ty.fullname(): String {
    return shortPresentableText(fq = true)
}

fun Ty.typeLabel(relativeTo: MoveElement): String {
    val typeModule = this.getDefiningModule()
    if (typeModule != null && typeModule != relativeTo.containingModule) {
        return this.fullname()
    } else {
        return this.name()
    }
//    val file = relativeTo.containingFile as? MoveFile
//    return shortPresentableText(file)
}

fun Ty.shortPresentableText(fq: Boolean = false): String =
    render(this,
           level = 3,
           fq = fq)

val Ty.insertionSafeText: String
    get() = render(this,
                   level = Int.MAX_VALUE,
                   unknown = "_",
                   anonymous = "_",
                   integer = "_")

fun tyToString(ty: Ty) = render(ty, Int.MAX_VALUE)

private fun render(
    ty: Ty,
    level: Int,
    unknown: String = "<unknown>",
    anonymous: String = "<anonymous>",
    integer: String = "{integer}",
    fq: Boolean = false
): String {
    check(level >= 0)
    if (ty is TyUnknown) return unknown
    if (ty is TyPrimitive) {
        return when (ty) {
            is TyBool -> "bool"
            is TyAddress -> "address"
            is TySigner -> "signer"
            is TyVector -> "vector"
            is TyUnit -> "()"
            is TyInteger -> {
                if (ty.kind == TyInteger.DEFAULT_KIND) {
                    "integer"
                } else {
                    ty.kind.toString()
                }
            }
            else -> error("unreachable")
        }
    }

    if (level == 0) return "_"

    val r = { subTy: Ty -> render(subTy, level - 1, unknown, anonymous, integer, fq) }

    return when (ty) {
        is TyFunction -> {
            val params = ty.paramTypes.joinToString(", ", "fn(", ")", transform = r)
            return if (ty.retType is TyUnit) params else "$params -> ${ty.retType}"
        }
//        is TySlice -> "[${r(ty.elementType)}]"

//        is TyTuple -> ty.types.joinToString(", ", "(", ")", transform = r)
        is TyVector -> "vector<${render(ty.item, level, unknown, anonymous, integer, fq)}>"
//        is TyVector -> "[${r(ty.item)}; ${ty.size ?: unknown}]"
        is TyReference -> "${if (ty.mutability.isMut) "&mut " else "&"}${
            render(ty.referenced, level, unknown, anonymous, integer, fq)
        }"
//        is TyPointer -> "*${if (ty.mutability.isMut) "mut" else "const"} ${r(ty.referenced)}"
//        is TyTraitObject -> ty.trait.name ?: anonymous
        is TyTypeParameter -> ty.name ?: anonymous
        is TyStruct -> {
            val name = if (fq) ty.item.fqName else (ty.item.name ?: anonymous)
//            val name = if (isFQName) {
//                ty.item.fqName
//            }
//            val name = ty.item.name ?: return anonymous
            val args =
                if (ty.typeArguments.isEmpty()) ""
                else ty.typeArguments.joinToString(", ", "<", ">", transform = r)
            name + args
        }
//        is TyStructOrEnumBase -> {
//            val name = ty.item.name ?: return anonymous
//        }
        is TyInfer -> when (ty) {
            is TyInfer.TyVar -> "?${ty.origin?.name ?: "_"}"
//            is TyInfer.IntVar -> integer
        }
//        is FreshTyInfer -> "<fresh>" // really should never be displayed; debug only
        else -> error("unreachable")
    }
}
