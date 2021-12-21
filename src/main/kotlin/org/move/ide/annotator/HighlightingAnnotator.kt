package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.move.ide.colors.MvColor
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

val INTEGER_TYPE_IDENTIFIERS = setOf("u8", "u64", "u128")
val PRIMITIVE_TYPE_IDENTIFIERS = INTEGER_TYPE_IDENTIFIERS + setOf("bool")
val PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS = setOf("address", "signer")
val BUILTIN_TYPE_IDENTIFIERS = PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS + setOf("vector")

val ACQUIRES_BUILTIN_FUNCTIONS = setOf("move_from", "borrow_global", "borrow_global_mut")
val BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE =
    ACQUIRES_BUILTIN_FUNCTIONS + setOf("exists", "freeze")
val BUILTIN_FUNCTIONS =
    BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE + setOf("assert", "move_to")

class HighlightingAnnotator : MvAnnotator() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val color = when {
            element is LeafPsiElement -> highlightLeaf(element)
            element is MvMacroIdent -> MvColor.MACRO
            element is MvLitExpr && element.text.startsWith("@") -> MvColor.ADDRESS
            else -> null
        } ?: return
        val severity = color.testSeverity
        holder.newSilentAnnotation(severity).textAttributes(color.textAttributesKey).create()
    }

    private fun highlightLeaf(element: PsiElement): MvColor? {
        val parent = element.parent as? MvElement ?: return null
        if (element.elementType.toString().endsWith("_kw")) return MvColor.KEYWORD
        return when {
            element.elementType == IDENTIFIER -> highlightIdentifier(parent)
            parent is MvCopyExpr
                    && element.text == "copy" -> MvColor.KEYWORD
            else -> null
        }
    }

    private fun highlightIdentifier(element: MvElement): MvColor? {
        if (element is MvAbility) return MvColor.ABILITY
        if (element is MvTypeParameter) return MvColor.TYPE_PARAMETER
        if (element is MvModuleRef && element.isSelf) return MvColor.KEYWORD
        if (element is MvItemImport && element.text == "Self") return MvColor.KEYWORD
        if (element is MvFunction) return MvColor.FUNCTION_DEF
        if (element is MvBindingPat && element.owner is MvConstDef) return MvColor.CONSTANT_DEF
        if (element is MvModuleDef) return MvColor.MODULE_DEF

        // any qual :: access is not highlighted
        if (element !is MvPathIdent || !element.isIdentifierOnly) return null

        val path = element.parent as? MvPath ?: return null
        val identifierName = path.identifierName
        val pathContainer = path.parent
        when (pathContainer) {
            is MvPathType -> {
                if (identifierName in PRIMITIVE_TYPE_IDENTIFIERS) return MvColor.PRIMITIVE_TYPE
                if (identifierName in BUILTIN_TYPE_IDENTIFIERS) return MvColor.BUILTIN_TYPE

                val resolved = path.reference?.resolve()
                if (resolved is MvTypeParameter) {
                    return MvColor.TYPE_PARAMETER
                }
            }
            is MvCallExpr -> {
                val resolved = path.reference?.resolve() as? MvFunction
                if (resolved != null) {
                    if (resolved.isNative && identifierName in BUILTIN_FUNCTIONS) {
                        return MvColor.BUILTIN_FUNCTION_CALL
                    } else {
                        return MvColor.FUNCTION_CALL
                    }
                }
            }
            is MvRefExpr -> {
                val resolved = path.reference?.resolve() as? MvBindingPat ?: return null
                val owner = resolved.owner
                when (owner) {
                    is MvConstDef -> return MvColor.CONSTANT
                    is MvLetStatement,
                    is MvFunctionParameter -> return MvColor.VARIABLE
                }
            }
        }
        return null
    }
}
