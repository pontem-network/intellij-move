package org.move.lang.utils

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.ide.annotator.MoveAnnotationHolder
import org.move.ide.presentation.shortPresentableText
import org.move.lang.MoveFile
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.startOffset
import org.move.lang.core.types.infer.TypeFoldable
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.ty.Ty
import org.move.lang.utils.Severity.*;

sealed class MoveDiagnostic(
    val element: PsiElement,
    val endElement: PsiElement? = null
) {
    abstract fun prepare(): PreparedAnnotation

    class TypeError(
        element: PsiElement,
        private val expectedTy: Ty,
        private val actualTy: Ty,
    ) : MoveDiagnostic(element), TypeFoldable<TypeError> {

        override fun prepare(): PreparedAnnotation {
            return PreparedAnnotation(
                ERROR,
                "Mismatched types",
                expectedFound(element, expectedTy, actualTy)
            )
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError =
            TypeError(element, expectedTy.foldWith(folder), actualTy.foldWith(folder))

        override fun innerVisitWith(visitor: TypeVisitor): Boolean =
            expectedTy.visitWith(visitor) || actualTy.visitWith(visitor)

        private fun expectedFound(element: PsiElement, expectedTy: Ty, actualTy: Ty): String {
            val file = element.containingFile as? MoveFile ?: return ""
            return "expected `${expectedTy.shortPresentableText(file)}`" +
                    ", found `${actualTy.shortPresentableText(file)}`"
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
    @Suppress("UnstableApiUsage") @InspectionMessage val header: String,
    @Suppress("UnstableApiUsage") @NlsContexts.Tooltip val description: String = "",
//    val fixes: List<LocalQuickFix> = emptyList(),
    val textAttributes: TextAttributesKey? = null
)

fun MoveDiagnostic.addToHolder(moveHolder: MoveAnnotationHolder) {
    val prepared = prepare()

    val textRange = if (endElement != null) {
        TextRange.create(
            element.startOffset,
            endElement.endOffset
        )
    } else {
        element.textRange
    }

    val holder = moveHolder.holder
    holder.newAnnotation(
        prepared.severity.toHighlightSeverity(),
        prepared.header
    )
        .tooltip(prepared.description)
        .highlightType(prepared.severity.toProblemHighlightType())
        .range(textRange)
        .create()

//    for (fix in prepared.fixes) {
//        if (fix is IntentionAction) {
//            ann.registerFix(fix)
//        } else {
//            val descriptor = InspectionManager.getInstance(element.project)
//                .createProblemDescriptor(
//                    element,
//                    endElement ?: element,
//                    ann.message,
//                    prepared.severity.toProblemHighlightType(),
//                    true,
//                    fix
//                )
//
//            ann.registerFix(fix, null, null, descriptor)
//        }
//    }
}
