package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.move.ide.colors.MoveColor
import org.move.lang.MoveElementTypes.ADDRESS_LITERAL
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.isNative

val INTEGER_TYPE_IDENTIFIERS = setOf("u8", "u64", "u128")
val PRIMITIVE_TYPE_IDENTIFIERS = INTEGER_TYPE_IDENTIFIERS + setOf("bool")
val PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS = setOf("address", "signer")
val BUILTIN_TYPE_IDENTIFIERS = PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS + setOf("vector")

val ACQUIRES_BUILTIN_FUNCTIONS = setOf("move_from", "borrow_global", "borrow_global_mut")
val BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE =
    ACQUIRES_BUILTIN_FUNCTIONS + setOf("exists", "freeze")
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
        if (element.elementType.toString().endsWith("_kw")) return MoveColor.KEYWORD
        return when {
            element.elementType == IDENTIFIER -> highlightIdentifier(parent)
            parent is MoveCopyExpr
                    && element.text == "copy" -> MoveColor.KEYWORD
            element.elementType == ADDRESS_LITERAL -> MoveColor.ADDRESS
            else -> null
        }
    }

    private fun highlightIdentifier(element: MoveElement): MoveColor? {
        if (element is MoveAbility) return MoveColor.ABILITY
        if (element is MoveTypeParameter) return MoveColor.TYPE_PARAMETER
        if (element is MoveModuleRef && element.isSelf) return MoveColor.KEYWORD
        if (element is MoveItemImport && element.text == "Self") return MoveColor.KEYWORD
        if (element is MoveFunctionSignature) return MoveColor.FUNCTION_DEF
        if (element is MoveBindingPat && element.owner is MoveConstDef) return MoveColor.CONSTANT_DEF
        if (element is MoveModuleDef) return MoveColor.MODULE_DEF

        // any qual :: access is not highlighted
        if (element !is MovePathIdent || !element.isIdentifierOnly) return null

        val path = element.parent as? MovePath ?: return null
        val identifierName = path.identifierName
        val pathContainer = path.parent
        when (pathContainer) {
            is MovePathType -> {
                if (identifierName in PRIMITIVE_TYPE_IDENTIFIERS) return MoveColor.PRIMITIVE_TYPE
                if (identifierName in BUILTIN_TYPE_IDENTIFIERS) return MoveColor.BUILTIN_TYPE

                val resolved = path.reference?.resolve()
                if (resolved is MoveTypeParameter) {
                    return MoveColor.TYPE_PARAMETER
                }
            }
            is MoveCallExpr -> {
                val resolved = path.reference?.resolve() as? MoveFunctionSignature
                if (resolved != null) {
                    if (resolved.isNative && identifierName in BUILTIN_FUNCTIONS) {
                        return MoveColor.BUILTIN_FUNCTION_CALL
                    } else {
                        return MoveColor.FUNCTION_CALL
                    }
                }
            }
            is MoveRefExpr -> {
                val resolved = path.reference?.resolve() as? MoveBindingPat ?: return null
                val owner = resolved.owner
                when (owner) {
                    is MoveConstDef -> return MoveColor.CONSTANT
                    is MoveLetStatement,
                    is MoveFunctionParameter -> return MoveColor.VARIABLE
                }
            }
        }
        return null
    }
}
