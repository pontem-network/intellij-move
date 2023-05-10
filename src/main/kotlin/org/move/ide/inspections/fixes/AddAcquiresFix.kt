package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.psiFactory

class AddAcquiresFix(
    function: MvFunction,
    val missingItems: List<String>
) :
    DiagnosticFix<MvFunction>(function) {

    override fun getFamilyName(): String = "Add missing acquires"
    override fun getText(): String = "Add missing `acquires ${missingItems.joinToString(", ")}`"

    override fun invoke(project: Project, file: PsiFile, element: MvFunction) {
        val acquiresType = element.acquiresType
        val psiFactory = project.psiFactory
        val missingItemsText = missingItems.joinToString(", ", "", "")
        if (acquiresType != null) {
            val acquires =
                psiFactory.acquires(acquiresType.text + ", " + missingItemsText)
            acquiresType.replace(acquires)
        } else {
            val acquires =
                psiFactory.acquires("acquires $missingItemsText")
            element.addBefore(acquires, element.codeBlock)
        }
    }
}
