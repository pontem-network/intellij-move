package org.move.utils

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement

sealed class MoveDiagnostic(
    val element: PsiElement,
    val endElement: PsiElement? = null
) {
    abstract fun prepare(): PreparedAnnotation


}

enum class Severity {
    INFO, WARN, ERROR, UNKNOWN_SYMBOL
}

private fun Severity.toProblemHighlightType(): ProblemHighlightType = when (this) {
    Severity.INFO -> ProblemHighlightType.INFORMATION
    Severity.WARN -> ProblemHighlightType.WEAK_WARNING
    Severity.ERROR -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    Severity.UNKNOWN_SYMBOL -> ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
}

class PreparedAnnotation(
    val severity: Severity,
    val header: String,
    val description: String = ""
)

//fun MoveDiagnostic.addToHolder(holder: ProblemsHolder) {
//    val prepared = prepare()
//    val descriptor = holder.manager.createProblemDescriptor(
//        element,
//        endElement ?: element,
//        "<html>${htmlHeader(prepared.errorCode, prepared.header)}<br>${prepared.description}</html>",
//        prepared.severity.toProblemHighlightType(),
//        holder.isOnTheFly
//    )
//    holder.registerProblem(descriptor)
//}
