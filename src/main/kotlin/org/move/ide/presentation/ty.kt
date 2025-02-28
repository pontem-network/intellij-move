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
    is TyAdt -> this.item.containingModule
    else -> null
}

fun Ty.nameNoArgs(): String {
    return this.name().replace(Regex("<.*>"), "")
}

fun Ty.name(colors: Boolean = false): String {
    return text(fq = false, colors = colors)
}

fun Ty.fullnameNoArgs(): String {
    return this.fullname().replace(Regex("<.*>"), "")
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

//val Ty.insertionSafeText: String
//    get() = render(
//        this,
//        level = Int.MAX_VALUE,
//        unknown = "_",
//        anonymous = "_",
//        integer = "_"
//    )

fun tyToString(ty: Ty) = render(ty, Int.MAX_VALUE)

private fun render(
    ty: Ty,
    level: Int,
    unknown: String = "<unknown>",
    anonymous: String = "<anonymous>",
    integer: String = "integer",
    fq: Boolean = false,
    toHtml: Boolean = false,
    typeParam: (TyTypeParameter) -> String = {
        it.name?.chainIf(toHtml) { colored(this, asTypeParam) }
            ?: anonymous
//        colored(it.name, asTypeParam, toHtml) ?: anonymous
    },
    tyVar: (TyInfer.TyVar) -> String = {
        val varName =
            it.origin?.name?.chainIf(toHtml) { colored(this, asTypeParam) }
                ?: "_${it.hashCode()}"
        "?$varName"
//        colored(it.origin?.name, asTypeParam, toHtml) ?: "_"
    },
): String {
    check(level >= 0)
    if (ty is TyUnknown) return unknown.chainIf(toHtml) { escapeForHtml() }
    if (ty is TyPrimitive) return renderPrimitive(ty, integer, toHtml = toHtml)

    if (level == 0) return "_"

    val r = { subTy: Ty -> render(subTy, level - 1, unknown, anonymous, integer, fq, toHtml, typeParam, tyVar) }

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
            buf += "&"
            if (ty.mutability.isMut) {
                buf += colored("mut", asKeyword, toHtml)
                buf += " "
            }
            buf += r(ty.referenced)
            buf.toString()
        }
        is TyTypeParameter -> typeParam(ty)
        is TyAdt -> {
            val itemName = if (fq) ty.item.fqName()?.editorText() ?: anonymous else (ty.item.name ?: anonymous)
            val typeArgs =
                if (ty.typeArguments.isEmpty()) ""
                else ty.typeArguments.joinToString(
                    ", ",
                    "<".escapeIf(toHtml),
                    ">".escapeIf(toHtml),
                    transform = r
                )
            itemName + typeArgs
//            itemName + typeArgs.chainIf(toHtml) { escapeForHtml() }
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
            val name = if (fq) ty.item.fqName()?.editorText() ?: anonymous else (ty.item.name ?: anonymous)
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

private fun renderPrimitive(ty: TyPrimitive, integer: String = "integer", toHtml: Boolean = false): String {
    val tyText = when (ty) {
        is TyBool -> "bool"
        is TyAddress -> "address"
        is TySigner -> "signer"
        is TyUnit -> "()"
        is TyNum -> "num"
        is TySpecBv -> "bv"
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
