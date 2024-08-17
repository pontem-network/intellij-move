package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.testAttrItem
import org.move.lang.core.psi.ext.valueArguments

/**
 * Fix that removes a parameter and all its usages at call sites.
 */
class RemoveParameterFix(
    binding: MvPatBinding,
    private val bindingName: String
) : DiagnosticFix<MvPatBinding>(binding) {

    override fun getText() = "Remove parameter `$bindingName`"
    override fun getFamilyName() = "Remove parameter"

    override fun invoke(project: Project, file: PsiFile, element: MvPatBinding) {
        val parameter = element.parent as? MvFunctionParameter ?: return
        val function = parameter.parentOfType<MvFunction>() ?: return

        val parameterIndex = function.parameters.indexOf(parameter)
        if (parameterIndex == -1) return

        parameter.deleteWithSurroundingCommaAndWhitespace()

        removeTestSignerAssignment(function, bindingName)
        removeArguments(function, parameterIndex)
    }
}

private fun removeArguments(function: MvFunction, parameterIndex: Int) {
    val calls = function.searchReferences()
        .asSequence()
        .mapNotNull {
            val path = it.element
            val pathExpr = path.parent
            pathExpr as? MvCallExpr
        }
    calls.forEach { call ->
        call.valueArguments
            .getOrNull(parameterIndex)
            ?.deleteWithSurroundingCommaAndWhitespace()
    }
}

private fun removeTestSignerAssignment(function: MvFunction, signerParameterName: String) {
    val testAttrItem = function.testAttrItem
    if (testAttrItem != null) {
        val attrItemList = testAttrItem.attrItemList
        if (attrItemList != null) {
            val signerAssigment =
                attrItemList.attrItemList.find { it.identifier.text == signerParameterName }
            signerAssigment?.deleteWithSurroundingCommaAndWhitespace()
            if (attrItemList.attrItemList.isEmpty()) {
                attrItemList.delete()
            }
        }
    }
}
