/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.search

import com.intellij.psi.PsiElement
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import org.move.lang.core.psi.MoveExpr

object MoveUsageTypeProvider : UsageTypeProviderEx {
    // Instantiate each UsageType only once, so that the equality check in UsageTypeGroup.equals() works correctly
//    private val IMPL = UsageType { "impl" }

//    private val TYPE_REFERENCE = UsageType { "type reference" }
//    private val TRAIT_REFERENCE = UsageType { "trait reference" }

    private val EXPR = UsageType { "expr" }
    private val DOT_EXPR = UsageType { "dot expr" }

    private val FUNCTION_CALL = UsageType { "function call" }

    //    private val METHOD_CALL = UsageType { "method call" }
    private val ARGUMENT = UsageType { "argument" }

//    private val MACRO_CALL = UsageType { "macro call" }
//    private val MACRO_ARGUMENT = UsageType { "macro argument" }

    private val INIT_STRUCT = UsageType { "init struct" }
    private val INIT_FIELD = UsageType { "init field" }

    private val PAT_BINDING = UsageType { "variable binding" }

//    private val FIELD = UsageType { "field" }

//    private val META_ITEM = UsageType { "meta item" }

    //    private val USE = UsageType { "use" }
    private val MOD = UsageType { "mod" }

    override fun getUsageType(element: PsiElement?): UsageType? =
        getUsageType(element, UsageTarget.EMPTY_ARRAY)

    override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
//        val refinedElement = element?.findExpansionElements()?.firstOrNull()?.parent ?: element
        val parent = element?.parent ?: return null;
        return when (parent) {
            is MoveExpr -> EXPR
            else -> null
        }
//        val parent = element?.goUpTillOtherThan<MovePathExpr>() ?: return null
//        return when (parent) {
////            is RsBaseType -> when (parent.parent) {
////                is RsImplItem -> IMPL
////                else -> TYPE_REFERENCE
////            }
//            is MoveExpr -> when (parent.goUpTillOtherThan<MovePathExpr>()) {
//                is MoveDotExpr -> DOT_EXPR
//                is MoveCallExpr -> FUNCTION_CALL
//                is MoveFunctionParams -> ARGUMENT
////                is RsFormatMacroArg -> MACRO_ARGUMENT
//                is MoveExpr -> EXPR
//                else -> null
//            }
////            is RsUseSpeck -> USE
//            is MoveStructLiteralExpr -> INIT_STRUCT
//            is MoveStructLiteralExprField -> INIT_FIELD
////            is RsTraitRef -> TRAIT_REFERENCE
////            is RsMethodCall -> METHOD_CALL
////            is RsMetaItem -> META_ITEM
////            is RsFieldLookup -> FIELD
////            is RsMacroCall -> MACRO_CALL
//            is MoveBindingPat -> PAT_BINDING
//            else -> when (parent.parent) {
//                is MoveModuleBlock -> MOD
//                else -> null
//            }
//        }
    }

//    private inline fun <reified T : PsiElement> PsiElement.goUpTillOtherThan(): PsiElement {
//        var context = this
//        while (context is T) {
//            context = context.parent
//        }
//        return context
//    }
}
