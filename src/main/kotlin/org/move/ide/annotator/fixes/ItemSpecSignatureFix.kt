package org.move.ide.annotator.fixes

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.utils.signature
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.ext.funcItem
import org.move.lang.core.psi.psiFactory

class ItemSpecSignatureFix(
    itemSpec: MvItemSpec
) :
    LocalQuickFixAndIntentionActionOnPsiElement(itemSpec) {

    override fun getText(): String = "Fix item signature"
    override fun getFamilyName(): String = text

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val itemSpec = startElement as? MvItemSpec ?: return
        val actualSignature = itemSpec.funcItem?.signature ?: return

        val psiFactory = project.psiFactory
        val newSpecSignature = psiFactory.itemSpecSignature(actualSignature)
        itemSpec.itemSpecSignature?.replace(newSpecSignature)
    }
}
