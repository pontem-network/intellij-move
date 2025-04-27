package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.TokenSet
import org.jaxen.expr.PathExpr
import org.move.cli.settings.moveSettings
import org.move.ide.colors.MvColor
import org.move.lang.MvElementTypes.*
import org.move.lang.core.CONTEXTUAL_KEYWORDS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.tokenSetOf
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyReference

val INTEGER_TYPE_IDENTIFIERS = setOf("u8", "u16", "u32", "u64", "u128", "u256")
val SPEC_INTEGER_TYPE_IDENTIFIERS = INTEGER_TYPE_IDENTIFIERS + setOf("num")
val SPEC_ONLY_PRIMITIVE_TYPES = setOf("num")
val PRIMITIVE_TYPE_IDENTIFIERS = INTEGER_TYPE_IDENTIFIERS + setOf("bool")
val PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS = setOf("address", "signer")
val BUILTIN_TYPE_IDENTIFIERS = PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS + setOf("vector")

val GLOBAL_STORAGE_ACCESS_FUNCTIONS =
    setOf("move_from", "borrow_global", "borrow_global_mut", "exists", "freeze")
val BUILTIN_FUNCTIONS =
    GLOBAL_STORAGE_ACCESS_FUNCTIONS + setOf("move_to")
val SPEC_BUILTIN_FUNCTIONS = setOf(
    "global", "len", "vec", "concat", "contains", "index_of", "range",
    "in_range", "update", "update_field", "old", "TRACE", "int2bv", "bv2int"
)

class HighlightingAnnotator: MvAnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        if (element.elementType == WHITE_SPACE) return
        val color = when {
            element is LeafPsiElement -> highlightLeaf(element)
            element is MvLitExpr && element.text.startsWith("@") -> MvColor.ADDRESS
            else -> null
        } ?: return
        val severity = color.testSeverity
        holder.newSilentAnnotation(severity)
            .textAttributes(color.textAttributesKey)
            .create()
    }

    private fun highlightLeaf(element: PsiElement): MvColor? {
        val parent = element.parent as? MvElement ?: return null
        val leafType = element.elementType
        if (leafType.toString().endsWith("_kw")) return MvColor.KEYWORD
        return when {
            leafType == IDENTIFIER -> highlightIdentifier(parent)
            leafType == QUOTE_IDENTIFIER -> MvColor.LABEL
            leafType == HEX_INTEGER_LITERAL -> MvColor.NUMBER
            parent is MvAssertMacroExpr -> MvColor.MACRO
            parent is MvCopyExpr && element.text == "copy" -> MvColor.KEYWORD

            // covers `#, [, ]`, in #[test(my_signer = @0x1)]
            parent is MvAttr -> MvColor.ATTRIBUTE
            // covers `(, )`, in #[test(my_signer = @0x1)]
            parent is MvAttrItemList -> MvColor.ATTRIBUTE
            // covers `=` in #[test(my_signer = @0x1)]
            parent is MvAttrItemInitializer -> MvColor.ATTRIBUTE

            leafType == COLON_COLON -> {
                val firstParent =
                    element.findFirstParent(true) { it is MvAttrItemInitializer || it is MvAttrItem }
                if (firstParent == null || firstParent is MvAttrItemInitializer) {
                    // belongs to the expression path, not highlighted
                    return null
                }
                // belongs to the attr item identifier
                return MvColor.ATTRIBUTE
            }

            else -> null
        }
    }

    private fun highlightIdentifier(element: MvElement): MvColor? {
        if (element is MvAssertMacroExpr) return MvColor.MACRO
        if (element is MvAbility) return MvColor.ABILITY
        if (element is MvTypeParameter) return MvColor.TYPE_PARAMETER
        if (element is MvItemSpecTypeParameter) return MvColor.TYPE_PARAMETER
        if (element is MvFunction)
            return when {
                element.isInline -> MvColor.INLINE_FUNCTION
                element.isView -> MvColor.VIEW_FUNCTION
                element.isEntry -> MvColor.ENTRY_FUNCTION
                element.selfParam != null -> MvColor.METHOD
                else -> MvColor.FUNCTION
            }
        if (element is MvStruct) return MvColor.STRUCT
        if (element is MvNamedFieldDecl) return MvColor.FIELD
        if (element is MvFieldLookup) return MvColor.FIELD
        if (element is MvMethodCall) return MvColor.METHOD_CALL
        if (element is MvPatFieldFull) return MvColor.FIELD
        if (element is MvPatField) return MvColor.FIELD
        if (element is MvStructLitField) return MvColor.FIELD
        if (element is MvConst) return MvColor.CONSTANT
        if (element is MvModule) return MvColor.MODULE
        if (element is MvVectorLitExpr) return MvColor.VECTOR_LITERAL
        if (element is MvAttrItem) return MvColor.ATTRIBUTE
        if (element is MvEnum) return MvColor.ENUM
        if (element is MvEnumVariant) return MvColor.ENUM_VARIANT

        return when (element) {
            is MvPath -> highlightPathElement(element)
            is MvPatBinding -> highlightBindingPat(element)
            else -> null
        }
    }

    private fun highlightBindingPat(bindingPat: MvPatBinding): MvColor {
        val bindingOwner = bindingPat.parent
        if (bindingPat.isMethodsEnabled &&
            bindingOwner is MvFunctionParameter && bindingOwner.isSelfParam
        ) {
            return MvColor.SELF_PARAMETER
        }
        val msl = bindingPat.isMslOnlyItem
        val itemTy = bindingPat.inference(msl)?.getBindingType(bindingPat)
        return if (itemTy != null) {
            highlightVariableByType(itemTy)
        } else {
            MvColor.VARIABLE
        }
    }

    private fun highlightPathElement(path: MvPath): MvColor? {
        val identifierName = path.identifierName
        if (identifierName == "Self") return MvColor.KEYWORD

        val rootPath = path.rootPath()
        if (rootPath.parent is MvAttrItem) return MvColor.ATTRIBUTE

        // any qual :: access is not highlighted
        if (path.qualifier != null) return null

        val pathOwner = path.parent
        return when (pathOwner) {
            is MvPathType -> {
                when {
                    identifierName in PRIMITIVE_TYPE_IDENTIFIERS -> MvColor.PRIMITIVE_TYPE
                    identifierName in SPEC_ONLY_PRIMITIVE_TYPES && path.isMslScope -> MvColor.PRIMITIVE_TYPE
                    identifierName in BUILTIN_TYPE_IDENTIFIERS -> MvColor.BUILTIN_TYPE
                    else -> {
                        val item = path.reference?.resolve()
                        when (item) {
                            is MvTypeParameter -> MvColor.TYPE_PARAMETER
                            is MvStruct -> MvColor.STRUCT
                            is MvEnum -> MvColor.ENUM
                            else -> null
                        }
                    }
                }
            }
            is MvStructLitExpr -> MvColor.STRUCT
            is MvPatStruct -> MvColor.STRUCT
            is MvPathExpr -> {
                val item = path.reference?.resolveFollowingAliases() ?: return null
                if (pathOwner.parent is MvCallExpr) {
                    return when {
                        item is MvSpecFunction
                                && item.isNative
                                && identifierName in SPEC_BUILTIN_FUNCTIONS -> MvColor.BUILTIN_FUNCTION_CALL
                        item is MvFunction
                                && item.isNative
                                && identifierName in BUILTIN_FUNCTIONS -> MvColor.BUILTIN_FUNCTION_CALL
                        item is MvFunction && item.isEntry -> MvColor.ENTRY_FUNCTION_CALL
                        item is MvFunction && item.isView -> MvColor.VIEW_FUNCTION_CALL
                        item is MvFunction && item.isInline -> MvColor.INLINE_FUNCTION_CALL
                        else -> MvColor.FUNCTION_CALL
                    }
                }
                when {
                    item is MvConst -> MvColor.CONSTANT
                    else -> {
                        val itemParent = item.parent
                        if (itemParent.isMethodsEnabled
                            && itemParent is MvFunctionParameter && itemParent.isSelfParam
                        ) {
                            MvColor.SELF_PARAMETER
                        } else {
                            val msl = path.isMslScope
                            val itemTy = pathOwner.inference(msl)?.getExprType(pathOwner)
                            if (itemTy != null) {
                                highlightVariableByType(itemTy)
                            } else {
                                MvColor.VARIABLE
                            }
                        }
                    }
                }
            }
            else -> null
        }
    }

    private fun highlightVariableByType(itemTy: Ty): MvColor =
        when {
            itemTy is TyReference -> {
                val referenced = itemTy.referenced
                when {
                    referenced is TyAdt && referenced.adtItem.hasKey ->
                        if (itemTy.isMut) MvColor.MUT_REF_TO_KEY_OBJECT else MvColor.REF_TO_KEY_OBJECT
                    referenced is TyAdt && referenced.adtItem.hasStore && !referenced.adtItem.hasDrop ->
                        if (itemTy.isMut) MvColor.MUT_REF_TO_STORE_NO_DROP_OBJECT else MvColor.REF_TO_STORE_NO_DROP_OBJECT
                    referenced is TyAdt && referenced.adtItem.hasStore && referenced.adtItem.hasDrop ->
                        if (itemTy.isMut) MvColor.MUT_REF_TO_STORE_OBJECT else MvColor.REF_TO_STORE_OBJECT
                    else ->
                        if (itemTy.isMut) MvColor.MUT_REF else MvColor.REF
                }
            }
            itemTy is TyAdt && itemTy.adtItem.hasStore && !itemTy.adtItem.hasDrop -> MvColor.STORE_NO_DROP_OBJECT
            itemTy is TyAdt && itemTy.adtItem.hasStore -> MvColor.STORE_OBJECT
            itemTy is TyAdt && itemTy.adtItem.hasKey -> MvColor.KEY_OBJECT
            else -> MvColor.VARIABLE
        }

    private val PsiElement.isMethodsEnabled
        get() =
            project.moveSettings.enableReceiverStyleFunctions

    companion object {
        private val HIGHLIGHTED_ELEMENTS = TokenSet.orSet(
            tokenSetOf(
                IDENTIFIER, QUOTE_IDENTIFIER
            ),
            CONTEXTUAL_KEYWORDS
        )
    }
}
