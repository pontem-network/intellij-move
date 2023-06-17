package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.HEX_INTEGER_LITERAL
import org.move.lang.core.psi.MvAddressLit
import org.move.lang.core.psi.MvLitExpr

sealed class Literal(open val element: PsiElement) {
    class Integer(element: PsiElement) : Literal(element)
    class HexInteger(element: PsiElement) : Literal(element)
    class ByteString(element: PsiElement) : Literal(element)
    class HexString(element: PsiElement) : Literal(element)
    class Bool(element: PsiElement) : Literal(element)
    class Address(override val element: MvAddressLit) : Literal(element)
}

val MvLitExpr.hexIntegerLiteral: PsiElement?
    get() =
        this.findFirstChildByType(HEX_INTEGER_LITERAL)

val MvLitExpr.literal: Literal
    get() = when {
        this.hexIntegerLiteral != null -> Literal.HexInteger(this.hexIntegerLiteral!!)
        this.integerLiteral != null -> Literal.Integer(this.integerLiteral!!)
        this.boolLiteral != null -> Literal.Bool(this.boolLiteral!!)
        this.byteStringLiteral != null -> Literal.ByteString(this.byteStringLiteral!!)
        this.hexStringLiteral != null -> Literal.HexString(this.hexStringLiteral!!)
        this.addressLit != null -> Literal.Address(this.addressLit!!)
        else -> error("unreachable")
    }
