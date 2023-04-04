package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.fixes.RemoveTestSignerFix
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvAttrItemArgument
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.ancestorStrict

class UnusedTestSignerInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object : MvVisitor() {
            override fun visitAttrItemArgument(itemArgument: MvAttrItemArgument) {
                val attr = itemArgument.ancestorStrict<MvAttrItem>() ?: return
                if (attr.name != "test") return
                val attrName = itemArgument.referenceName ?: return
                if (!itemArgument.resolvable) {
                    holder.registerProblem(
                        itemArgument,
                        "Unused test signer",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        RemoveTestSignerFix(itemArgument, attrName)
                    )
                }
            }
        }
}
