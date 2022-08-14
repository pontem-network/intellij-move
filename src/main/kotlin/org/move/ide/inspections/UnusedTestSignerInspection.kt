package org.move.ide.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.lang.MvElementTypes.COMMA
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvAttrItemArgument
import org.move.lang.core.psi.MvAttrItemArguments
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.getNextNonCommentSibling
import org.move.lang.core.psi.ext.getPrevNonCommentSibling

class UnusedTestSignerInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object : MvVisitor() {
            override fun visitAttrItemArgument(o: MvAttrItemArgument) {
                val attr = o.ancestorStrict<MvAttrItem>() ?: return
                if (attr.name != "test") return
                val attrName = o.referenceName ?: return
                if (!o.resolvable) {
                    holder.registerProblem(
                        o,
                        "Unused test signer",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        object : InspectionQuickFix("Remove '$attrName'") {
                            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                val attrArgument = descriptor.psiElement as MvAttrItemArgument

                                // remove trailing comma
                                attrArgument.getNextNonCommentSibling()
                                    ?.takeIf { it.elementType == COMMA }
                                    ?.delete()

                                // remove previous comma if this is last element
                                val container =
                                    descriptor.psiElement.parent as MvAttrItemArguments
                                val index = container.attrItemArgumentList.indexOf(attrArgument)
                                if (index == container.attrItemArgumentList.size - 1) {
                                    attrArgument.getPrevNonCommentSibling()
                                        ?.takeIf { it.elementType == COMMA }
                                        ?.delete()
                                }

                                descriptor.psiElement.delete()
                            }
                        }
                    )
                }
            }
        }
}
