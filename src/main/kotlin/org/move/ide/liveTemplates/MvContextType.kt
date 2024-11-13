package org.move.ide.liveTemplates

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.move.ide.MvHighlighter
import org.move.lang.MoveLanguage
import org.move.lang.core.psi.MvBlockFields
import org.move.lang.core.psi.MvCodeBlock
import org.move.lang.core.psi.MvEnumBody
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvType

sealed class MvContextType(presentableName: String): TemplateContextType(presentableName) {

    final override fun isInContext(context: TemplateActionContext): Boolean {
        if (!PsiUtilCore.getLanguageAtOffset(context.file, context.startOffset).isKindOf(MoveLanguage)) {
            return false
        }

        val element = context.file.findElementAt(context.startOffset)
        if (element == null || element is PsiComment) {
            return false
        }

        return isInContext(element)
    }

    protected abstract fun isInContext(element: PsiElement): Boolean

    override fun createHighlighter(): SyntaxHighlighter = MvHighlighter()

    class Generic: MvContextType("Move") {
        override fun isInContext(element: PsiElement) = true
    }

    class Module: MvContextType("Module") {
        override fun isInContext(element: PsiElement): Boolean = owner(element) is MvModule
    }

    class Block: MvContextType("Code block") {
        override fun isInContext(element: PsiElement): Boolean = owner(element) is MvCodeBlock
    }

    class Type: MvContextType("Type") {
        override fun isInContext(element: PsiElement): Boolean = owner(element) is MvType
    }

    companion object {
        private fun owner(element: PsiElement): PsiElement? = PsiTreeUtil.findFirstParent(element) {
            it is MvCodeBlock
                    || it is MvModule
                    || it is PsiFile
                    || it is MvType
                    // filter out enum/struct body
                    || it is MvEnumBody || it is MvBlockFields
        }
    }
}
