package org.move.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.testAttr
import org.move.lang.core.psi.ext.valueArguments

/**
 * Fix that removes a parameter and all its usages at call sites.
 */
class RemoveParameterFix(
    binding: MvBindingPat,
    private val bindingName: String
) : LocalQuickFixOnPsiElement(binding) {
    override fun getText() = "Remove parameter `$bindingName`"
    override fun getFamilyName() = "Remove parameter"

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val binding = startElement as? MvBindingPat ?: return
        val parameter = binding.parent as? MvFunctionParameter ?: return
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

private fun removeTestSignerAssignment(function: MvFunction, parameterName: String) {
    val testAttr = function.testAttr
    if (testAttr != null) {
        val testAttrItem = testAttr.attrItemList.single()
        val attrArguments = testAttrItem.attrItemArguments
        if (attrArguments != null) {
            val signerAssigment =
                attrArguments.attrItemArgumentList.find { it.identifier.text == parameterName }
            signerAssigment?.deleteWithSurroundingCommaAndWhitespace()
            if (attrArguments.attrItemArgumentList.isEmpty()) {
                attrArguments.delete()
            }
        }
    }
}
