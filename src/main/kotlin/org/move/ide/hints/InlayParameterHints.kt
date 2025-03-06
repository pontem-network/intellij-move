package org.move.ide.hints

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvPathExpr
import org.move.lang.core.psi.MvStructLitExpr
import org.move.lang.core.psi.ext.MvCallable
import org.move.lang.core.psi.ext.argumentExprs
import org.move.lang.core.psi.ext.startOffset

@Suppress("UnstableApiUsage")
object InlayParameterHints {
    fun provideHints(element: PsiElement): List<InlayInfo> {
        if (element !is MvCallable) return emptyList()
        val callInfo = CallInfo.resolve(element) ?: return emptyList()
        val parameters = callInfo.parameters
        return parameters
            .mapNotNull { it.name }
            .zip(element.argumentExprs)
            .asSequence()
            .filter { (_, arg) -> arg != null }
            // don't show argument, if just function call / variable / struct literal
            //            .filter { (_, arg) -> arg !is MvRefExpr && arg !is MvCallExpr && arg !is MvStructLitExpr }
            .filter { (_, arg) -> arg !is MvPathExpr && arg !is MvStructLitExpr }
            .filter { (hint, arg) -> !isSimilar(hint, arg!!.text) }
            .filter { (hint, _) -> hint != "_" }
            .map { (hint, arg) -> InlayInfo("$hint:", arg!!.startOffset) }
            .toList()
    }

    private fun isSimilar(hint: String, argumentText: String): Boolean {
        val argText = argumentText.lowercase()
        val hintText = hint.lowercase()
        return argText.startsWith(hintText) || argText.endsWith(hintText)
    }
}
