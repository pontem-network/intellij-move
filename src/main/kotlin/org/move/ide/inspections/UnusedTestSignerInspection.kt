package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.fixes.RemoveTestSignerFix
import org.move.lang.core.psi.MvAttr
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvVisitor

class UnusedTestSignerInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object: MvVisitor() {
            override fun visitAttrItem(attrItem: MvAttrItem) {
                // stop if not a top-level item
                if (attrItem.parent !is MvAttr) return
                // stop if not a #[test]
                if (attrItem.referenceName != "test") return

                val innerAttrItems = attrItem.attrItemList?.attrItemList.orEmpty()
                for (innerAttrItem in innerAttrItems) {
                    val refName = innerAttrItem.referenceName ?: continue
                    if (innerAttrItem.unresolved) {
                        holder.registerProblem(
                            innerAttrItem,
                            "Unused test signer",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            RemoveTestSignerFix(innerAttrItem, refName)
                        )
                    }
                }
            }
        }
}
