package org.move.ide.presentation

import com.intellij.util.applyIf
import io.ktor.util.*
import org.move.ide.docs.DocumentationUtils.asBuiltin
import org.move.ide.docs.DocumentationUtils.asKeyword
import org.move.ide.docs.DocumentationUtils.asPrimitiveTy
import org.move.ide.docs.DocumentationUtils.colorize
import org.move.ide.docs.DocumentationUtils.leftAngle
import org.move.ide.docs.DocumentationUtils.rightAngle
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingModule
import org.move.lang.core.types.ty.*

// null -> builtin module
fun Ty.declaringModule(): MvModule? = when (this) {
    is TyReference -> this.referenced.declaringModule()
    is TyAdt -> this.item.containingModule
    else -> null
}

fun Ty.nameNoArgs(): String {
    return this.name().replace(Regex("<.*>"), "")
}

fun Ty.name(): String {
    return text(fq = false)
}

fun Ty.colorizedName(): String {
    return text(fq = false, colors = true)
}

fun Ty.fullnameNoArgs(): String {
    return this.fullname().replace(Regex("<.*>"), "")
}

fun Ty.fullname(): String {
    return text(fq = true)
}

fun Ty.colorizedFullname(): String {
    return text(fq = true, colors = true)
}

fun Ty.typeLabel(relativeTo: MvElement): String {
    val typeModule = this.declaringModule()
    return if (typeModule != null && typeModule != relativeTo.containingModule) {
        this.colorizedFullname()
    } else {
        this.colorizedName()
    }
}

fun Ty.hintText(): String =
    render(this, level = 3, unknown = "?", tyVar = { "?" })

fun Ty.text(fq: Boolean = false, colors: Boolean = false): String =
    render(
        this,
        level = 3,
        fq = fq,
        colors = colors,
    )

fun Ty.expectedTyText(): String {
    return render(
        this,
        level = 3,
        typeParam = {
            val name = it.origin.name ?: "_"
            val abilities =
                it.abilities()
                    .toList().sorted()
                    .joinToString(", ", "(", ")") { a -> a.label() }
            "$name$abilities"
        },
        tyVar = {
            val name = it.origin?.name ?: "_"
            val abilities =
                it.abilities()
                    .toList().sorted()
                    .joinToString(", ", "(", ")") { a -> a.label() }
            "?$name$abilities"
        },
        fq = true
    )
}

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
    typeParam: (TyTypeParameter) -> String = { it.name ?: anonymous },
    tyVar: (TyInfer.TyVar) -> String = { "?${it.origin?.name ?: "_"}" },
    fq: Boolean = false,
    colors: Boolean = false
): String {
    check(level >= 0)
    if (ty is TyUnknown) return unknown
    if (ty is TyPrimitive) {
        return when (ty) {
            is TyBool -> colorize("bool", asPrimitiveTy, !colors)
            is TyAddress -> colorize("address", asBuiltin, !colors)
            is TySigner -> colorize("signer", asBuiltin, !colors)
            is TyUnit -> "()"
            is TyNum -> colorize("num", asPrimitiveTy, !colors)
            is TySpecBv -> "bv"
            is TyInteger -> {
                colorize(if (ty.kind == TyInteger.DEFAULT_KIND) {
                    integer
                } else {
                    ty.kind.toString()
                }, asPrimitiveTy, !colors)
            }
            is TyNever -> "<never>"
            else -> error("unreachable")
        }
    }

    if (level == 0) return "_"

    val r = { subTy: Ty -> render(subTy, level - 1, unknown, anonymous, integer, typeParam, tyVar, fq, colors) }

    return when (ty) {
        is TyFunction -> {
            val params = ty.paramTypes.joinToString(", ", "fn(", ")", transform = r)
            var s = if (ty.returnType is TyUnit) params else "$params -> ${r(ty.returnType)}"
            if (ty.acquiresTypes.isNotEmpty()) {
                s += ty.acquiresTypes.joinToString(", ", " acquires ", transform = r)
            }
            s
        }
        is TyTuple -> ty.types.joinToString(", ", "(", ")", transform = r)
        is TyVector -> {
            buildString {
                colorize("vector", asBuiltin, !colors)
                append("<".applyIf(colors) { leftAngle })
                append(r(ty.item))
                append(">".applyIf(colors) { rightAngle })
            }
        }
        is TyRange -> colorize("range", asBuiltin, !colors) + "<${r(ty.item)}>".applyIf(colors) { this.escapeHTML() }
        is TyReference -> {
            val prefix = if (ty.mutability.isMut) "&" + colorize("mut ", asKeyword, !colors) else "&"
            "$prefix${r(ty.referenced)}"
        }
        is TyTypeParameter -> {
            typeParam(ty)
        }
//        is TyStruct -> {
//            val name = if (fq) ty.item.qualName?.editorText() ?: anonymous else (ty.item.name ?: anonymous)
//            val args =
//                if (ty.typeArguments.isEmpty()) ""
//                else ty.typeArguments.joinToString(", ", "<", ">", transform = r)
//            name + args
//        }
        is TyAdt -> {
            val name = if (fq) ty.item.qualName?.editorText() ?: anonymous else (ty.item.name ?: anonymous)
            val args =
                if (ty.typeArguments.isEmpty()) ""
                else ty.typeArguments.joinToString(", ", "<", ">", transform = r)
            name + args.applyIf(colors) { this.escapeHTML() }
        }
        is TyInfer -> when (ty) {
            is TyInfer.TyVar -> tyVar(ty)
            is TyInfer.IntVar -> integer
        }
        is TyLambda -> {
            val params = ty.paramTypes.joinToString(",", "|", "|", transform = r)
            val retType = if (ty.returnType is TyUnit)
                "()"
            else
                r(ty.returnType)
            "$params -> $retType"
        }
        is TySchema -> {
            val name = if (fq) ty.item.qualName?.editorText() ?: anonymous else (ty.item.name ?: anonymous)
            val args =
                if (ty.typeArguments.isEmpty()) ""
                else ty.typeArguments.joinToString(", ", "<", ">", transform = r)
            name + args.applyIf(colors) { this.escapeHTML() }
        }
        else -> error("unimplemented for type ${ty.javaClass.name}")
    }
}
