package org.move.ide.inspections.fixes

import com.intellij.model.SideEffectGuard
import com.intellij.model.SideEffectGuard.EffectType.SETTINGS
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.ktor.http.*
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.DiagnosticIntentionFix
import org.move.ide.inspections.fixes.CompilerV2Feat.*

enum class CompilerV2Feat(val title: String) {
    INDEXING("Index notation"),
    RESOURCE_CONTROL("Resource access control"),
    PUBLIC_PACKAGE("`public(package)` function visibility");
}

class EnableCompilerV2FeatureFix(
    element: PsiElement,
    val feature: CompilerV2Feat
):
    DiagnosticIntentionFix<PsiElement>(element) {

    override fun getText(): String =
        "Enable ${feature.title.quote()} feature of Aptos Move V2 Compiler in the settings"

    override fun invoke(project: Project, file: PsiFile, element: PsiElement) {
        @Suppress("UnstableApiUsage")
        SideEffectGuard.checkSideEffectAllowed(SETTINGS)
        project.moveSettings.modify {
            when (feature) {
                INDEXING -> it.enableIndexExpr = true
                RESOURCE_CONTROL -> it.enableResourceAccessControl = true
                PUBLIC_PACKAGE -> it.enablePublicPackage = true
            }
        }
    }
}