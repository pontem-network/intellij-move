package org.move.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.move.lang.MoveFile
import org.move.lang.core.psi.MoveVisitor
import org.move.openapiext.common.isUnitTestMode

abstract class MoveLocalInspectionTool: LocalInspectionTool() {
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

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        buildMoveVisitor(holder, isOnTheFly) ?: super.buildVisitor(holder, isOnTheFly)

    abstract fun buildMoveVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MoveVisitor

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
        return true
//        return file.cargoWorkspace != null
//                && file.crateRoot != null
//                && file.project.toolchain?.looksLikeValidToolchain() == true
    }
}
