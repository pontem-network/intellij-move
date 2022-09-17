package org.move.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.move.cli.moveProjects
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
        return file.project.moveProjects.findMoveProject(file) != null
    }
}

abstract class InspectionQuickFix(val fixName: String) : LocalQuickFix {
    override fun getFamilyName(): String = fixName
}

abstract class MvLocalQuickFixOnPsiElement<T: PsiElement>(psiElement: T): LocalQuickFixOnPsiElement(psiElement) {
    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        @Suppress("UNCHECKED_CAST")
        val element = startElement as T
        if (stillApplicable(project, file, element)) {
            invoke(project, file, element)
        }
    }

    abstract fun stillApplicable(project: Project, file: PsiFile, element: T): Boolean

    abstract fun invoke(project: Project, file: PsiFile, element: T)
}
