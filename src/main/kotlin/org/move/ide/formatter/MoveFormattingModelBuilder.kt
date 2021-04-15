package org.move.ide.formatter

import com.intellij.formatting.*

class MoveFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val formatterBlock = MoveFormatterBlock(
            formattingContext.psiElement.node,
            null,
            null,
            Indent.getNoneIndent(),
            MoveFmtContext.create(formattingContext.codeStyleSettings)
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.containingFile,
                                                                       formatterBlock,
                                                                       formattingContext.codeStyleSettings)
    }
}