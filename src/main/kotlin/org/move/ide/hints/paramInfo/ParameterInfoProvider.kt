package org.move.ide.hints.paramInfo

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

interface ParameterInfoProvider {
    val targetElementType: IElementType
    fun findParameterInfo(listElement: PsiElement): ParametersInfo?

    interface ParametersInfo {
        val presentText: String
        fun getRangeInParent(index: Int): TextRange
    }
}