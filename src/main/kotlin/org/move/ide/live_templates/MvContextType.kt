package org.move.ide.live_templates

import com.intellij.codeInsight.template.EverywhereContextType
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
import org.move.lang.core.psi.MvCodeBlock
import org.move.lang.core.psi.MvModule
import kotlin.reflect.KClass

sealed class MvContextType(
    id: String,
    presentableName: String,
    baseContextType: KClass<out TemplateContextType>
) : TemplateContextType(id, presentableName, baseContextType.java) {

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

    class Generic : MvContextType("MOVE_FILE", "Move", EverywhereContextType::class) {
        override fun isInContext(element: PsiElement) = true
    }

    class Module : MvContextType("MOVE_MODULE", "Module", Generic::class) {
        override fun isInContext(element: PsiElement): Boolean
        // inside MvModuleDef
                = owner(element) is MvModule
    }

    class Block : MvContextType("MOVE_BLOCK", "Block", Generic::class) {
        override fun isInContext(element: PsiElement): Boolean
        // inside MvCodeBlock
                = owner(element) is MvCodeBlock
    }

    companion object {
        private fun owner(element: PsiElement): PsiElement? = PsiTreeUtil.findFirstParent(element) {
            it is MvCodeBlock || it is MvModule || it is PsiFile
        }
    }
}
