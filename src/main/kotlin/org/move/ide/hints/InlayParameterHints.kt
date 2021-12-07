package org.move.ide.hints

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import org.move.ide.utils.CallInfo
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.ext.startOffset

@Suppress("UnstableApiUsage")
object InlayParameterHints {
    fun provideHints(elem: PsiElement): List<InlayInfo> {
        if (elem !is MvCallExpr) return emptyList()

        val callInfo = CallInfo.resolve(elem) ?: return emptyList()
        val arguments = elem.callArgumentList?.exprList ?: return emptyList()

        return callInfo.parameters
            .map { it.name }
            .zip(arguments)
            .filter { (hint, arg) -> !isSimilar(hint, arg.text) }
            .filter { (hint, _) -> hint != "_" }
            .map { (hint, arg) -> InlayInfo("$hint:", arg.startOffset) }
    }

    private fun isSimilar(hint: String, argumentText: String): Boolean {
        val argText = argumentText.lowercase()
        val hintText = hint.lowercase()
        return argText.endsWith(hintText) || argText.startsWith(hintText)
    }
}
