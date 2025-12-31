package org.move.ide.inspections.fixes

import com.intellij.model.SideEffectGuard
import com.intellij.model.SideEffectGuard.EffectType.SETTINGS
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.DiagnosticIntentionFix

class EnableMoveV2Fix(element: PsiElement): DiagnosticIntentionFix<PsiElement>(element) {

    override fun getText(): String = "Enable Endless Move V2 in the settings"

    override fun invoke(project: Project, file: PsiFile, element: PsiElement) {
        @Suppress("UnstableApiUsage")
        SideEffectGuard.checkSideEffectAllowed(SETTINGS)
        project.moveSettings.modify { it.enableMove2 = true }

    }
}