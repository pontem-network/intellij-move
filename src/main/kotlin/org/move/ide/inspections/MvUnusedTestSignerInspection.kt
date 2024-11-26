package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.fixes.RemoveTestSignerFix
import org.move.lang.core.psi.MvAttr
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.unqualifiedName

class MvUnusedTestSignerInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object: MvVisitor() {
            override fun visitAttrItem(attrItem: MvAttrItem) {
                // stop if not a top-level item
                if (attrItem.parent !is MvAttr) return
                // stop if not a #[test]
                if (!attrItem.isTest) return

                val innerAttrItems = attrItem.attrItemList?.attrItemList.orEmpty()
                for (innerAttrItem in innerAttrItems) {
                    val refName = innerAttrItem.unqualifiedName ?: continue
                    if (innerAttrItem.path.unresolved) {
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
