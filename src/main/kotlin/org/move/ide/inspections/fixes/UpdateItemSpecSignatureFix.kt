package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.inspections.MvLocalQuickFixOnPsiElement
import org.move.lang.core.psi.MvItemSpec

class UpdateItemSpecSignatureFix(itemSpec: MvItemSpec) : MvLocalQuickFixOnPsiElement<MvItemSpec>(itemSpec) {

    override fun getFamilyName(): String {
        TODO("Not yet implemented")
    }

    override fun getText(): String {
        TODO("Not yet implemented")
    }

    override fun stillApplicable(project: Project, file: PsiFile, element: MvItemSpec): Boolean {
        TODO("Not yet implemented")
    }

    override fun invoke(project: Project, file: PsiFile, element: MvItemSpec) {
        TODO("Not yet implemented")
    }

}
