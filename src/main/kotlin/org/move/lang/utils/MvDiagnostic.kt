package org.move.lang.utils

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.ide.annotator.MvAnnotationHolder
import org.move.ide.annotator.fixes.ItemSpecSignatureFix
import org.move.ide.annotator.fixes.WrapWithParensExprFix
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.ext.*
import org.move.lang.utils.Severity.*

sealed class MvDiagnostic(
    val element: PsiElement,
    val textRange: TextRange
) {
    constructor(element: PsiElement) : this(element, element.textRange)

    constructor(element: PsiElement, endElement: PsiElement) :
            this(
                element,
                TextRange(
                    element.startOffset,
                    endElement.endOffset
                )
            )

    abstract fun prepare(): PreparedAnnotation

//    class TypeError(
//        element: PsiElement,
//        private val expectedTy: Ty,
//        private val actualTy: Ty,
//    ) : MvDiagnostic(element), TypeFoldable<TypeError> {
//
//        override fun prepare(): PreparedAnnotation {
//            return PreparedAnnotation(
//                ERROR,
//                "Mismatched types",
//                expectedFound(expectedTy, actualTy)
//            )
//        }
//
//        override fun innerFoldWith(folder: TypeFolder): TypeError =
//            TypeError(element, expectedTy.foldWith(folder), actualTy.foldWith(folder))
//
//        override fun visitWith(visitor: TypeVisitor): Boolean = innerVisitWith(visitor)
//
//        override fun innerVisitWith(visitor: TypeVisitor): Boolean =
//            expectedTy.visitWith(visitor) || actualTy.visitWith(visitor)
//
//        private fun expectedFound(expectedTy: Ty, actualTy: Ty): String {
//            return "expected `${expectedTy.text(true)}`" +
//                    ", found `${actualTy.text(true)}`"
//        }
//    }

    class TypeArgumentsNumberMismatch(
        element: PsiElement,
        private val label: String,
        private val expectedCount: Int,
        private val realCount: Int,
    ) : MvDiagnostic(element) {

        override fun prepare(): PreparedAnnotation {
            val errorText = "Invalid instantiation of '$label'. " +
                    "Expected $expectedCount type argument(s) but got $realCount"
            return PreparedAnnotation(
                ERROR,
                errorText,
            )
        }
    }

    class NeedsTypeAnnotation(element: PsiElement) : MvDiagnostic(element) {
        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "Could not infer this type. Try adding an annotation",
            )
        }
    }

    class StorageAccessIsNotAllowed(
        path: MvPath,
        private val typeName: String,
    ) : MvDiagnostic(path) {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "The type '$typeName' was not declared in the current module. " +
                        "Global storage access is internal to the module"
            )
        }
    }

    class FunctionSignatureMismatch(itemSpec: MvItemSpec) :
        MvDiagnostic(
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

    class ParensAreRequiredForCastExpr(castExpr: MvCastExpr) : MvDiagnostic(castExpr) {
        override fun prepare(): PreparedAnnotation {
            val castExpr = element as MvCastExpr
            return PreparedAnnotation(
                ERROR,
                "Parentheses are required for the cast expr",
                fixes = listOf(WrapWithParensExprFix(castExpr))
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
    @Suppress("UnstableApiUsage") @NlsContexts.Tooltip val description: String = "",
    val fixes: List<LocalQuickFix> = emptyList(),
//    val textAttributes: TextAttributesKey? = null
)

fun MvDiagnostic.addToHolder(moveHolder: MvAnnotationHolder) {
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
