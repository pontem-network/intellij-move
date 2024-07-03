package org.move.ide.inspections

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.*
import com.intellij.model.SideEffectGuard
import com.intellij.model.SideEffectGuard.EffectType.EXEC
import com.intellij.model.SideEffectGuard.EffectType.SETTINGS
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.move.cli.moveProjectsService
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvVisitor
import org.move.openapiext.common.isUnitTestMode

abstract class MvLocalInspectionTool : LocalInspectionTool() {
    final override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        val file = session.file
        return if (file is MoveFile && isApplicableTo(file)) {
            buildVisitor(holder, isOnTheFly)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        buildMvVisitor(holder, isOnTheFly)

    abstract fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor

    open val isSyntaxOnly: Boolean = false

    /**
     * Syntax-only inspections are applicable to any [MoveFile].
     *
     * Other inspections should analyze only files that:
     * - belong to a workspace
     * - are included in module tree, i.e. have a crate root
     * - belong to a project with a configured and valid Rust toolchain
     */
    private fun isApplicableTo(file: MoveFile): Boolean {
        if (isUnitTestMode) return true
        if (isSyntaxOnly) return true
        return file.project.moveProjectsService.findMoveProjectForPsiElement(file) != null
    }
}

abstract class DiagnosticFix<T : PsiElement>(element: T) : LocalQuickFixOnPsiElement(element) {

    val targetElement: T? get() = this.startElement

    override fun getStartElement(): T? {
        @Suppress("UNCHECKED_CAST")
        return super.getStartElement() as T?
    }

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun getFamilyName(): String = text

    final override fun isAvailable(
        project: Project,
        file: PsiFile,
        startElement: PsiElement,
        endElement: PsiElement
    ): Boolean {
        val element = getStartElement() ?: return false
        return stillApplicable(element) && stillApplicable(project, file, element)
    }

    final override fun invoke(
        project: Project,
        file: PsiFile,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val element = getStartElement() ?: return
        if (stillApplicable(element)
            && stillApplicable(project, file, element)
        ) {
            invoke(project, file, element)
        }
    }

    /**
     * Convenience method to add simple condition which depends only on the element itself.
     */
    open fun stillApplicable(element: T): Boolean = true

    open fun stillApplicable(project: Project, file: PsiFile, element: T): Boolean = true

    abstract fun invoke(project: Project, file: PsiFile, element: T)
}

abstract class DiagnosticIntentionFix<T : PsiElement>(element: T) :
    LocalQuickFixAndIntentionActionOnPsiElement(element) {

    override fun getStartElement(): T {
        @Suppress("UNCHECKED_CAST")
        return super.getStartElement() as T
    }

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun getFamilyName(): String = text

    override fun isAvailable(
        project: Project,
        file: PsiFile,
        startElement: PsiElement,
        endElement: PsiElement
    ): Boolean = stillApplicable(project, file, getStartElement())

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        @Suppress("UNCHECKED_CAST")
        val element = startElement as T
        if (stillApplicable(project, file, element)) {
            invoke(project, file, element)
        }
    }

    open fun stillApplicable(project: Project, file: PsiFile, element: T): Boolean = true

    abstract fun invoke(project: Project, file: PsiFile, element: T)
}
