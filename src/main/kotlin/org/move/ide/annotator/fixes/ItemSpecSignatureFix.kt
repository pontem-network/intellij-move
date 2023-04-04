package org.move.ide.annotator.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticIntentionFix
import org.move.ide.utils.signature
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.ext.funcItem
import org.move.lang.core.psi.psiFactory

class ItemSpecSignatureFix(
    itemSpec: MvItemSpec
) :
    DiagnosticIntentionFix<MvItemSpec>(itemSpec) {

    override fun getText(): String = "Fix item signature"

    override fun invoke(project: Project, file: PsiFile, element: MvItemSpec) {
        val itemSpec = startElement
        val actualSignature = itemSpec.funcItem?.signature ?: return

        val psiFactory = project.psiFactory
        val newSpecSignature = psiFactory.itemSpecSignature(actualSignature)
        itemSpec.itemSpecSignature?.replace(newSpecSignature)
    }
}
