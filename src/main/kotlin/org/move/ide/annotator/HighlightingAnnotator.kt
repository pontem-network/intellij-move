package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import org.move.ide.colors.MoveColor
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.identifierName
import org.move.lang.core.psi.ext.isIdentifierOnly
import org.move.lang.core.psi.ext.isSelf

val INTEGER_TYPE_IDENTIFIERS = setOf("u8", "u64", "u128")
val PRIMITIVE_TYPE_IDENTIFIERS = INTEGER_TYPE_IDENTIFIERS + setOf("bool")
val BUILTIN_TYPE_IDENTIFIERS = setOf("address", "signer", "vector")

val BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE =
    setOf("move_from", "borrow_global", "borrow_global_mut", "exists", "freeze")
val BUILTIN_FUNCTIONS =
    BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE + setOf("assert", "move_to")

class HighlightingAnnotator : MoveAnnotator() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val color = when (element) {
            is LeafPsiElement -> highlightLeaf(element)
            else -> null
        } ?: return
        val severity = color.testSeverity
        holder.newSilentAnnotation(severity).textAttributes(color.textAttributesKey).create()
    }

    private fun highlightLeaf(element: PsiElement): MoveColor? {
        val parent = element.parent as? MoveElement ?: return null
        return when (element.elementType) {
            IDENTIFIER -> highlightIdentifier(parent)
            else -> null
        }
    }

    private fun highlightIdentifier(element: MoveElement): MoveColor? {
        if (element is MoveAbility) return MoveColor.IDENTIFIER
        if (element is MoveTypeParameter) return MoveColor.TYPE_PARAMETER
        if (element is MoveModuleRef && element.isSelf) return MoveColor.KEYWORD

        if (element is MoveQualPath && element.isIdentifierOnly) {
            val name = element.identifierName
            val container = element.parent
            if (container is MoveQualPathType) {
                val resolved = container.reference?.resolve()
                if (resolved is MoveTypeParameter) {
                    return MoveColor.TYPE_PARAMETER
                }
            }
            return when {
                container is MoveQualPathType
                        && name in PRIMITIVE_TYPE_IDENTIFIERS -> MoveColor.PRIMITIVE_TYPE
                container is MoveQualPathType
                        && name in BUILTIN_TYPE_IDENTIFIERS -> MoveColor.BUILTIN_TYPE
                container is MoveCallExpr
                        && name in BUILTIN_FUNCTIONS -> MoveColor.BUILTIN_FUNCTION
                else -> null
            }
        } else {
            return null
        }
    }
}
