package org.move.ide.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import org.move.ide.docs.MvColorUtils.asBuiltinType
import org.move.ide.docs.MvColorUtils.asKeyword
import org.move.ide.docs.MvColorUtils.asPrimitiveType
import org.move.ide.docs.MvColorUtils.asTypeParam
import org.move.ide.docs.MvColorUtils.colored
import org.move.ide.docs.escapeForHtml
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingModule
import org.move.lang.core.types.fqName
import org.move.lang.core.types.ty.*
import org.move.stdext.chainIf

// null -> builtin module
fun Ty.declaringModule(): MvModule? = when (this) {
    is TyReference -> this.referenced.declaringModule()
    is TyAdt -> this.adtItem.containingModule
    else -> null
}

fun Ty.name(colors: Boolean = false): String {
    return text(fq = false, colors = colors)
}

private val TYPE_ARGS_REGEX = Regex("<.*>")

fun Ty.fullnameNoArgs(): String {
    val fullname = render(
        this,
        level = 1,
        fq = true,
        toHtml = false,
    )
    return fullname.replace(TYPE_ARGS_REGEX, "")
}

fun Ty.fullname(colors: Boolean = false): String {
    return text(fq = true, colors = colors)
}

fun Ty.hintText(): String =
    render(this, level = 3, unknown = "?", tyVar = { "?" })

fun Ty.text(fq: Boolean = false, colors: Boolean = false): String =
    render(
        this,
        level = 3,
        fq = fq,
        toHtml = colors,
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

fun tyToString(ty: Ty) = render(ty, Int.MAX_VALUE)

private fun render(
    ty: Ty,
    level: Int,
    unknown: String = "<unknown>",
    anonymous: String = "<anonymous>",
    fq: Boolean = false,
    toHtml: Boolean = false,
    typeParam: (TyTypeParameter) -> String = {
        it.name?.chainIf(toHtml) { colored(this, asTypeParam) } ?: anonymous
    },
    tyVar: (TyInfer.TyVar) -> String = {
        val varName =
            it.origin?.name?.chainIf(toHtml) { colored(this, asTypeParam) } ?: "_${it.hashCode()}"
        "?$varName"
    },
): String {
    check(level >= 0)
    if (ty is TyUnknown) return unknown.chainIf(toHtml) { escapeForHtml() }
    if (ty is TyPrimitive) return renderPrimitive(ty, toHtml = toHtml)

    if (level == 0) return "_"

    val r = { subTy: Ty -> render(subTy, level - 1, unknown, anonymous, fq, toHtml, typeParam, tyVar) }

    return when (ty) {
        is TyCallable -> {
            when (ty.kind) {
                is CallKind.Lambda -> {
                    val params = ty.paramTypes.joinToString(",", "|", "|", transform = r)
                    val retType = if (ty.returnType is TyUnit)
                        "()"
                    else
                        r(ty.returnType)
                    "$params -> $retType"
                }
                is CallKind.Function -> {
                    val params = ty.paramTypes.joinToString(", ", "fn(", ")", transform = r)
                    if (ty.returnType is TyUnit) {
                        params
                    } else {
                        "$params -> ${r(ty.returnType)}"
                    }
                }
            }
        }
        is TyTuple -> ty.types.joinToString(", ", "(", ")", transform = r)
        is TyVector -> {
            val buf = StringBuilder()
            buf.colored("vector", asBuiltinType, noHtml = !toHtml)
            buf += "<".escapeIf(toHtml) + r(ty.item) + ">".escapeIf(toHtml)
            buf.toString()
        }
        is TyRange -> {
            val buf = StringBuilder()
            buf.colored("range", asBuiltinType, noHtml = !toHtml)
            buf += "<".escapeIf(toHtml) + r(ty.item) + ">".escapeIf(toHtml)
            buf.toString()
        }
        is TyReference -> {
            val buf = StringBuilder()
            // todo: escape?
            buf += "&".escapeIf(toHtml)
            if (ty.mutability.isMut) {
                buf += colored("mut", asKeyword, toHtml)
                buf += " "
            }
            buf += r(ty.referenced)
            buf.toString()
        }
        is TyTypeParameter -> typeParam(ty)
        is TyAdt -> {
            val itemName = if (fq) ty.adtItem.fqName()?.identifierText() ?: anonymous else (ty.adtItem.name ?: anonymous)
            val typeArgs =
                if (ty.typeArguments.isEmpty()) ""
                else ty.typeArguments.joinToString(
                    ", ",
                    "<".escapeIf(toHtml),
                    ">".escapeIf(toHtml),
                    transform = r
                )
            itemName + typeArgs
        }
        is TyInfer -> when (ty) {
            is TyInfer.TyVar -> tyVar(ty)
            is TyInfer.IntVar -> "?integer"
        }
        is TySchema -> {
            val name = if (fq) ty.item.fqName()?.identifierText() ?: anonymous else (ty.item.name ?: anonymous)
            val typeArgs =
                if (ty.typeArguments.isEmpty()) {
                    ""
                } else {
                    ty.typeArguments.joinToString(
                        ", ",
                        "<".escapeIf(toHtml),
                        ">".escapeIf(toHtml),
                        transform = r
                    )
                }
            name + typeArgs
        }
        else -> error("unimplemented for type ${ty.javaClass.name}")
    }
}

private fun renderPrimitive(ty: TyPrimitive, toHtml: Boolean = false): String {
    val tyText = when (ty) {
        is TyBool -> "bool"
        is TyAddress -> "address"
        is TySigner -> "signer"
        is TyUnit -> "()"
        is TyNum -> "num"
        is TySpecBv -> "bv"
        is TyInteger -> {
            if (ty.kind == TyInteger.DEFAULT_KIND) {
                "integer"
            } else {
                ty.kind.toString()
            }
        }
        is TyNever -> "<never>"
        else -> error("unreachable")
    }
        .chainIf(toHtml) { escapeForHtml() }

    return colored(tyText, asPrimitiveType, toHtml)!!
}

private fun String.escapeIf(toHtml: Boolean) = this.chainIf(toHtml) { escapeForHtml() }

private fun colored(text: String, color: TextAttributes): String {
    return colored(text, color, html = true)!!
}

private fun colored(text: String?, color: TextAttributes, html: Boolean = true): String? {
    if (text == null) return null
    val buf = StringBuilder()
    buf.colored(text, color, !html)
    return buf.toString()
}

operator fun StringBuilder.plusAssign(value: String?) {
    if (value != null) {
        append(value)
    }
}
