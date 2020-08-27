package org.move.ide.formatter

import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings

class MvFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
        val ctx = MvFmtContext.create(settings)
        val block = MvFormatterBlock(element.node, null, null, Indent.getNoneIndent(), ctx)
        return FormattingModelProvider.createFormattingModelForPsiFile(
            element.containingFile,
            block,
            settings
        )
    }
}