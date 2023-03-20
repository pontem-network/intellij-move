package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.move.lang.core.psi.*

class RedundantQualifiedPathInspection : MvLocalInspectionTool() {

    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object : MvVisitor() {
            override fun visitPath(path: MvPath) {
                val pathText = path.text
                    .replace(path.typeArgumentList?.text.orEmpty(), "")
                    .replace(Regex("\\s"), "")
                val item = path.reference?.resolveWithAliases() ?: return

                val importsOwner = path.containingScript?.scriptBlock
                    ?: path.containingModule?.moduleBlock
                    ?: return
                val shortestPathText = importsOwner.shortestPathText(item) ?: return
                // if aliases are involved, could lead to bugs
                if (!pathText.endsWith(shortestPathText)) return

                val diff = pathText.length - shortestPathText.length
                if (diff > 0) {
                    if (pathText.substring(0, diff) == "Self::") return
                    val range = TextRange.from(0, diff)
                    holder.registerProblem(
                        path,
                        "Redundant qualifier",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        range,
                        object : LocalQuickFix {
                            override fun getFamilyName(): String = "Remove redundant qualifier"

                            override fun applyFix(
                                project: Project,
                                descriptor: ProblemDescriptor
                            ) {
                                val newPath = project.psiFactory.path(shortestPathText)
                                descriptor.psiElement.replace(newPath)
                            }

                        })
                }
            }
        }
}
