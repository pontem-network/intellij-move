package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvLitExpr
import org.move.lang.core.psi.MvParensExpr
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.Literal
import org.move.lang.core.psi.ext.literal
import org.move.lang.core.psi.ext.startOffset
import org.move.lang.utils.Diagnostic
import org.move.lang.utils.addToHolder

/*
    Augments parser to make the error messages better.
 */
class MvSyntaxErrorAnnotator : MvAnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MvAnnotationHolder(holder)
        val visitor = object : MvVisitor() {
            override fun visitLitExpr(expr: MvLitExpr) = checkLitExpr(moveHolder, expr)
            override fun visitCastExpr(expr: MvCastExpr) = checkCastExpr(moveHolder, expr)
        }
        element.accept(visitor)
    }

    private fun checkCastExpr(holder: MvAnnotationHolder, castExpr: MvCastExpr) {
        val parent = castExpr.parent
        if (parent !is MvParensExpr) {
            Diagnostic
                .ParensAreRequiredForCastExpr(castExpr)
                .addToHolder(holder)
        }
    }

    private fun checkLitExpr(holder: MvAnnotationHolder, litExpr: MvLitExpr) {
        val lit = litExpr.literal
        when (lit) {
            is Literal.HexInteger -> {
                val litValue = lit.element.text
                var actualLitValue = litValue.removePrefix("0x").lowercase()
                val actualLitOffset = lit.element.startOffset + 2
                if (actualLitValue.isEmpty()) {
                    holder.createErrorAnnotation(litExpr, "Invalid hex integer")
                    return
                }
                val match = HEX_INTEGER_WITH_SUFFIX_REGEX.matchEntire(actualLitValue)
                if (match != null) {
                    actualLitValue = match.groups[1]!!.value.lowercase()
                    val (suffix, range) = match.groups[2]!!
                    if (suffix !in ACCEPTABLE_INTEGER_SUFFIXES) {
                        val suffixRange = TextRange.from(actualLitOffset + range.first, suffix.length)
                        holder.createErrorAnnotation(
                            suffixRange,
                            "Invalid hex integer suffix"
                        )
                    }
                }
                for ((i, char) in actualLitValue.toList().withIndex()) {
                    if (char !in ACCEPTABLE_HEX_SYMBOLS) {
                        val offset = actualLitOffset + i
                        holder.createErrorAnnotation(
                            TextRange.from(offset, 1),
                            "Invalid hex integer symbol"
                        )
                    }
                }
            }
            is Literal.Integer -> {
                var litValue = lit.element.text.lowercase()
                val match = INTEGER_WITH_SUFFIX_REGEX.matchEntire(litValue)
                if (match != null) {
                    litValue = match.groups[1]!!.value.lowercase()
                    val (suffix, range) = match.groups[2]!!
                    if (suffix !in ACCEPTABLE_INTEGER_SUFFIXES) {
                        val litOffset = lit.element.startOffset
                        val suffixRange = TextRange.from(litOffset + range.first, suffix.length)
                        holder.createErrorAnnotation(
                            suffixRange,
                            "Invalid integer suffix"
                        )
                    }
                }
                for ((i, char) in litValue.toList().withIndex()) {
                    if (char !in ACCEPTABLE_INTEGER_SYMBOLS) {
                        val offset = lit.element.startOffset + i
                        holder.createErrorAnnotation(
                            TextRange.from(offset, 1),
                            "Invalid integer symbol"
                        )
                    }
                }
            }
            is Literal.HexString -> {
                val litValue = lit.element.text.lowercase()
                if (!litValue.endsWith('"')) {
                    // don't check incomplete hex strings
                    return
                }
                val hexValue = litValue.removePrefix("x\"").removeSuffix("\"")
                val hexValueOffset = lit.element.startOffset + 2

                // check hex string has even number of symbols
                if (hexValue.length % 2 != 0) {
                    holder.createErrorAnnotation(
                        TextRange.from(hexValueOffset, hexValue.length),
                        "Odd number of characters in hex string. " +
                                "Expected 2 hexadecimal digits for each byte"
                    )
                }

                // check all symbols are valid
                for ((i, char) in hexValue.toList().withIndex()) {
                    if (char !in ACCEPTABLE_HEX_SYMBOLS) {
                        val offset = hexValueOffset + i
                        holder.createErrorAnnotation(
                            TextRange.from(offset, 1),
                            "Invalid hex symbol"
                        )
                    }
                }
            }
            else -> Unit
        }
    }

    companion object {
        private val INTEGER_WITH_SUFFIX_REGEX =
            Regex("([0-9a-zA-Z]+)(u[0-9]{1,4})")
        private val ACCEPTABLE_INTEGER_SYMBOLS =
            setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        private val ACCEPTABLE_INTEGER_SUFFIXES =
            setOf("u8", "u16", "u32", "u64", "u128", "u256")
        private val HEX_INTEGER_WITH_SUFFIX_REGEX =
            Regex("([0-9a-zA-Z]+)*(u[0-9]{1,4})")
        private val ACCEPTABLE_HEX_SYMBOLS =
            setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
//        private val INTEGER_WITH_SUFFIX_REGEX =
//            Regex("([0-9a-zA-Z]+)(u(8)|(16)|(32)|(64)|(128)|(256))")

//        private val INTEGER_REGEX = Regex("[0-9]+(u(8)|(16)|(32)|(64)|(128)|(256))?")
    }
}
