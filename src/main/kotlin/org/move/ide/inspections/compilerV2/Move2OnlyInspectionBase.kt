package org.move.ide.inspections.compilerV2

import com.intellij.codeInspection.ProblemsHolder
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.MvLocalInspectionTool
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvVisitor

abstract class Move2OnlyInspectionBase<TElement: MvElement>(
    val elementClass: Class<TElement>
): MvLocalInspectionTool() {

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object: MvVisitor() {
            override fun visitElement(o: MvElement) {
                super.visitElement(o)
                if (!elementClass.isInstance(o) || o.textLength == 0) return

                // disable for move v1
                if (!o.project.moveSettings.enableMove2) return

                @Suppress("UNCHECKED_CAST")
                visitTargetElement(o as TElement, holder, isOnTheFly)
            }
        }
    }

    abstract fun visitTargetElement(element: TElement, holder: ProblemsHolder, isOnTheFly: Boolean)
}