package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import org.move.ide.colors.MoveColor
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.MoveQualifiedPath
import org.move.lang.core.psi.MoveRefExpr

val PRIMITIVE_TYPE_IDENTIFIERS = setOf("signer", "u8", "u64", "u128", "address", "bool")

class BuiltinTypesHighlightingAnnotator : AnnotatorBase() {
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

    private fun highlightIdentifier(element: PsiElement, parent: MoveElement): MoveColor? {
        val isPrimitiveType = parent is MoveQualifiedPath && element.text in PRIMITIVE_TYPE_IDENTIFIERS
        return when {
            isPrimitiveType -> MoveColor.PRIMITIVE_TYPE
            else -> null
        }
    }

//    private fun highlightBuiltinType(holder: AnnotationHolder, type: MoveType) {
//        val color = MoveColor.PRIMITIVE_TYPE;
//        val severity = color.testSeverity;
//        holder.newSilentAnnotation(severity).textAttributes(color.textAttributesKey).create()
//    }
}