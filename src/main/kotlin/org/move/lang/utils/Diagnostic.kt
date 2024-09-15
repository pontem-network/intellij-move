package org.move.lang.utils

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil.pluralize
import com.intellij.psi.PsiElement
import org.move.ide.annotator.MvAnnotationHolder
import org.move.ide.annotator.fixes.ItemSpecSignatureFix
import org.move.ide.annotator.fixes.WrapWithParensExprFix
import org.move.ide.annotator.pluralise
import org.move.ide.inspections.fixes.EnableMoveV2Fix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.utils.Severity.*

sealed class Diagnostic(
    val element: PsiElement,
    val textRange: TextRange
) {
    constructor(element: PsiElement): this(element, element.textRange)

    abstract fun prepare(): PreparedAnnotation

    class DuplicateDefinitions(element: PsiElement, val name: String): Diagnostic(element) {
        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(ERROR, "Duplicate definitions with name `${name}`")
        }
    }

    class TypeArgumentsNumberMismatch(
        element: PsiElement,
        private val label: String,
        private val expectedCount: Int,
        private val realCount: Int,
    ): Diagnostic(element) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "Invalid instantiation of '$label'. " +
                        "Expected $expectedCount type argument(s) but got $realCount",
            )
        }
    }

    class NoTypeArgumentsExpected(
        element: PsiElement,
        private val itemLabel: String,
    ): Diagnostic(element) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "No type arguments expected for '$itemLabel'"
            )
        }
    }

    class ValueArgumentsNumberMismatch(
        target: PsiElement,
        private val expectedCount: Int,
        private val realCount: Int,
    ): Diagnostic(target) {
        override fun prepare(): PreparedAnnotation {
            val errorMessage =
                "This function takes " +
                        "$expectedCount ${pluralise(expectedCount, "parameter", "parameters")} " +
                        "but $realCount ${pluralise(realCount, "parameter", "parameters")} " +
                        "${pluralise(realCount, "was", "were")} supplied"
            return PreparedAnnotation(
                ERROR,
                errorMessage
            )
        }
    }

    class NeedsTypeAnnotation(element: PsiElement): Diagnostic(element) {
        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "Could not infer this type. Try adding an annotation",
            )
        }
    }

    sealed class StorageAccessError(path: MvPath): Diagnostic(path) {
        class WrongModule(
            path: MvPath,
            private val correctModuleName: String,
            private val typeName: String,
        ): StorageAccessError(path) {

            override fun prepare(): PreparedAnnotation {
                return PreparedAnnotation(
                    ERROR,
                    "Invalid operation: storage operation on type `$typeName` can only be done " +
                            "within the defining module `$correctModuleName`",
                )
            }
        }

        class WrongItem(
            path: MvPath,
            private val itemName: String,
        ): StorageAccessError(path) {
            override fun prepare(): PreparedAnnotation {
                return PreparedAnnotation(
                    ERROR,
                    "Expected a struct type. Global storage operations are restricted to struct types " +
                            "declared in the current module. Found: `$itemName`",
                )
            }
        }
    }


    class FunctionSignatureMismatch(itemSpec: MvItemSpec):
        Diagnostic(
            itemSpec,
            TextRange(
                itemSpec.itemSpecRef?.startOffset ?: itemSpec.startOffset,
                itemSpec.itemSpecSignature?.endOffset
                    ?: itemSpec.itemSpecBlock?.startOffset?.dec()
                    ?: itemSpec.endOffset
            )
        ) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                WARN,
                "Function signature mismatch",
                fixes = listOf(ItemSpecSignatureFix(element as MvItemSpec))
            )
        }
    }

    class ParensAreRequiredForCastExpr(castExpr: MvCastExpr): Diagnostic(castExpr) {
        override fun prepare(): PreparedAnnotation {
            val castExpr = element as MvCastExpr
            return PreparedAnnotation(
                ERROR,
                "Parentheses are required for the cast expr",
                fixes = listOf(WrapWithParensExprFix(castExpr))
            )
        }
    }

//    class NativeStructNotSupported(struct: MvStruct, errorRange: TextRange): Diagnostic(struct, errorRange) {
//        override fun prepare(): PreparedAnnotation {
//            return PreparedAnnotation(
//                ERROR,
//                "Native structs aren't supported by the Move VM anymore"
//            )
//        }
//    }

    class SpecFunctionRequiresReturnType(specFunction: MvSpecFunction):
        Diagnostic(
            specFunction,
            TextRange.create(
                specFunction.functionParameterList?.endOffset
                    ?: specFunction.specCodeBlock?.startOffset
                    ?: specFunction.startOffset,
                (specFunction.specCodeBlock?.startOffset?.plus(1)) ?: specFunction.endOffset
            )
        ) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "Requires return type"
            )
        }
    }

    class EntryFunCannotHaveReturnValue(returnType: MvReturnType): Diagnostic(returnType) {
        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "Entry functions cannot have a return value"
            )
        }
    }

    class IndexExprIsNotSupportedInCompilerV1(indexExpr: MvIndexExpr): Diagnostic(indexExpr) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "Index operator is not supported in Aptos Move V1 outside specs",
                fixes = listOf(EnableMoveV2Fix(element))
            )
        }
    }

    class PublicPackageIsNotSupportedInCompilerV1(modifier: MvVisibilityModifier): Diagnostic(modifier) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "public(package) is not supported in Aptos Move V1",
                fixes = listOf(EnableMoveV2Fix(element))
            )
        }
    }

    class ReceiverStyleFunctionsIsNotSupportedInCompilerV1(methodCall: MvMethodCall): Diagnostic(methodCall) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "receiver-style functions are not supported in Aptos Move V1",
                fixes = listOf(EnableMoveV2Fix(element))
            )
        }
    }

    class PackageAndFriendModifiersCannotBeUsedTogether(modifier: MvVisibilityModifier): Diagnostic(modifier) {
        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "public(package) and public(friend) cannot be used together in the same module"
            )
        }
    }

    class MissingFieldsInTuplePattern(
        pat: MvPat,
        private val declaration: MvFieldsOwner,
        private val expectedAmount: Int,
        private val actualAmount: Int
    ): Diagnostic(pat) {

        override fun prepare(): PreparedAnnotation {
            val itemType = if (declaration is MvEnumVariant) "Enum variant" else "Tuple struct"
            return PreparedAnnotation(
                ERROR,
                "$itemType pattern does not correspond to its declaration: " +
                        "expected $expectedAmount ${pluralize("field", expectedAmount)}, found $actualAmount"
            )
        }
    }

    class MissingFieldsInStructPattern(
        patStruct: MvPatStruct,
        private val declaration: MvFieldsOwner,
        private val missingFields: List<MvFieldDecl>
    ): Diagnostic(patStruct.path.referenceNameElement!!) {

        override fun prepare(): PreparedAnnotation {
            val itemType = if (declaration is MvEnumVariant) "Enum variant" else "Struct"
            val missingFieldNames = missingFields.joinToString(", ") { "`${it.name!!}`" }
            return PreparedAnnotation(
                ERROR,
                "$itemType pattern does not mention " +
                        "${pluralize("field", missingFields.size)} $missingFieldNames"
            )
        }
    }
}

enum class Severity {
    INFO, WARN, ERROR, UNKNOWN_SYMBOL
}

private fun Severity.toProblemHighlightType(): ProblemHighlightType = when (this) {
    INFO -> ProblemHighlightType.INFORMATION
    WARN -> ProblemHighlightType.WEAK_WARNING
    ERROR -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    UNKNOWN_SYMBOL -> ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
}

private fun Severity.toHighlightSeverity(): HighlightSeverity = when (this) {
    INFO -> HighlightSeverity.INFORMATION
    WARN -> HighlightSeverity.WARNING
    ERROR, UNKNOWN_SYMBOL -> HighlightSeverity.ERROR
}


class PreparedAnnotation(
    val severity: Severity,
    @InspectionMessage val header: String,
    @NlsContexts.Tooltip val description: String = "",
    val fixes: List<LocalQuickFix> = emptyList(),
//    val textAttributes: TextAttributesKey? = null
)

fun Diagnostic.addToHolder(moveHolder: MvAnnotationHolder) {
    val prepared = prepare()

    val holder = moveHolder.holder
    val ann = holder.newAnnotation(
        prepared.severity.toHighlightSeverity(),
        prepared.header
    )
    if (prepared.description.isNotBlank()) {
        ann.tooltip(prepared.description)
    }
    ann.highlightType(prepared.severity.toProblemHighlightType())
        .range(textRange)

    val message = prepared.description
    for (fix in prepared.fixes) {
        if (fix is IntentionAction) {
            ann.withFix(fix)
        } else {
            val descriptor = InspectionManager.getInstance(element.project)
                .createProblemDescriptor(
                    element,
                    element,
                    message,
                    prepared.severity.toProblemHighlightType(),
                    true,
                    fix
                )
            ann.newLocalQuickFix(fix, descriptor).registerFix()
        }
    }
    ann.create()
}
