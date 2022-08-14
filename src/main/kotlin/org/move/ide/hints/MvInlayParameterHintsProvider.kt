package org.move.ide.hints

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement

@Suppress("UnstableApiUsage")
class MvInlayParameterHintsProvider : InlayParameterHintsProvider {

    override fun getDefaultBlackList(): Set<String> = emptySet()

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        return InlayParameterHints.provideHints(element)
    }

    override fun getInlayPresentation(inlayText: String): String = inlayText
}
