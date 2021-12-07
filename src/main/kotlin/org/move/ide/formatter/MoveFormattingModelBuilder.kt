package org.move.ide.formatter

import com.intellij.formatting.*

class MvFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val formatterBlock = MvFormatterBlock(
            formattingContext.psiElement.node,
            null,
            null,
            Indent.getNoneIndent(),
            MvFmtContext.create(formattingContext.codeStyleSettings)
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.containingFile,
                                                                       formatterBlock,
                                                                       formattingContext.codeStyleSettings)
    }
}
