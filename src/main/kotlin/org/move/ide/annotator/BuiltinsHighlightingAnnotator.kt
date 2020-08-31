package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import org.move.ide.colors.MoveColor
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.MoveQualifiedPath
import org.move.lang.core.psi.ext.identifierName
import org.move.lang.core.psi.ext.isPlainIdentifier

val PRIMITIVE_TYPE_IDENTIFIERS = setOf("signer", "u8", "u64", "u128", "address", "bool")
val BUILTIN_FUNCTIONS =
    setOf("move_from", "move_to", "borrow_global", "borrow_global_mut", "exists", "freeze", "assert")

class BuiltinsHighlightingAnnotator : AnnotatorBase() {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val color = when (element) {
            is LeafPsiElement -> highlightLeaf(element)
//            is RsAttr -> RsColor.ATTRIBUTE
            else -> null
        } ?: return
        val severity = color.testSeverity;
        holder.newSilentAnnotation(severity).textAttributes(color.textAttributesKey).create()

//        val visitor = object : MoveVisitor() {
//            override fun visitType(o: MoveType) = highlightBuiltinType(holder, o)
//        }
//        element.accept(visitor)
    }

    private fun highlightLeaf(element: PsiElement): MoveColor? {
        val parent = element.parent as? MoveElement ?: return null
        return when (element.elementType) {
            IDENTIFIER -> highlightIdentifier(element, parent)
            else -> null
        }
    }

    private fun highlightIdentifier(element: PsiElement, parent: MoveElement): MoveColor? =
        if (parent is MoveQualifiedPath && parent.isPlainIdentifier) {
            when (parent.identifierName) {
                in PRIMITIVE_TYPE_IDENTIFIERS -> MoveColor.PRIMITIVE_TYPE
                in BUILTIN_FUNCTIONS -> MoveColor.BUILTIN_FUNCTION
                else -> null
            }
        } else null
}