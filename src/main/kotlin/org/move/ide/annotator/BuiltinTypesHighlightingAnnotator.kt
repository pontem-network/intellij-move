package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import org.move.ide.colors.MvColor
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvPathExpr
import org.move.lang.core.psi.ext.MvElement

val PRIMITIVE_TYPE_IDENTIFIERS = setOf("signer", "u8", "u64", "u128", "address")

class BuiltinTypesHighlightingAnnotator : AnnotatorBase() {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
//        val color = highlightLeaf(element, holder) ?: return
        val color = when (element) {
            is LeafPsiElement -> highlightLeaf(element)
//            is RsAttr -> RsColor.ATTRIBUTE
            else -> null
        } ?: return
        val severity = color.testSeverity;
        holder.newSilentAnnotation(severity).textAttributes(color.textAttributesKey).create()

//        val visitor = object : MvVisitor() {
//            override fun visitType(o: MvType) = highlightBuiltinType(holder, o)
//        }
//        element.accept(visitor)
    }

    private fun highlightLeaf(element: PsiElement): MvColor? {
        val parent = element.parent as? MvElement ?: return null
        return when (element.elementType) {
            IDENTIFIER -> highlightIdentifier(element, parent)
            else -> null
        }
    }

    private fun highlightIdentifier(element: PsiElement, parent: MvElement): MvColor? {
        val isPrimitiveType = parent is MvPathExpr && element.text in PRIMITIVE_TYPE_IDENTIFIERS
        return when {
            isPrimitiveType -> MvColor.PRIMITIVE_TYPE
            else -> null
        }
    }

//    private fun highlightBuiltinType(holder: AnnotationHolder, type: MvType) {
//        val color = MvColor.PRIMITIVE_TYPE;
//        val severity = color.testSeverity;
//        holder.newSilentAnnotation(severity).textAttributes(color.textAttributesKey).create()
//    }
}