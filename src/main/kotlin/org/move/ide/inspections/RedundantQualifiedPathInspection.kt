package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.move.lang.core.psi.*

class RedundantQualifiedPathInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object : MvVisitor() {
            override fun visitPathIdent(pathIdent: MvPathIdent) {
                val pathIdentText = pathIdent.text
                val item = pathIdent.parent.reference?.resolve() as? MvNamedElement ?: return

                val importsOwner = (pathIdent.containingScript ?: pathIdent.containingModule) ?: return
                val shortestPathIdentText = importsOwner.shortestPathIdentText(item) ?: return
                val diff = pathIdentText.length - shortestPathIdentText.length
                if (diff > 0) {
                    if (pathIdentText.substring(0, diff) == "Self::") return
                    val range = TextRange.from(0, diff)
                    holder.registerProblem(
                        pathIdent,
                        "Redundant qualifier",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        range,
                        object : LocalQuickFix {
                            override fun getFamilyName(): String = "Remove redundant qualifier"

                            override fun applyFix(
                                project: Project,
                                descriptor: ProblemDescriptor
                            ) {
                                val newPathIdent = project.psiFactory.createPathIdent(
                                    shortestPathIdentText
                                )
                                descriptor.psiElement.replace(newPathIdent)
                            }

                        })
                }
            }
        }
}
