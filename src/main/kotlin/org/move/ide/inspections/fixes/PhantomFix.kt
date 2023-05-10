package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.ext.isPhantom
import org.move.lang.core.psi.psiFactory

sealed class PhantomFix(typeParam: MvTypeParameter) : DiagnosticFix<MvTypeParameter>(typeParam) {

    class Add(typeParameter: MvTypeParameter) : PhantomFix(typeParameter) {

        override fun getFamilyName(): String = "Declare phantom"
        override fun getText(): String = "Declare phantom"

        override fun stillApplicable(project: Project, file: PsiFile, element: MvTypeParameter): Boolean =
            !element.isPhantom

        override fun invoke(project: Project, file: PsiFile, element: MvTypeParameter) {
            val newText = "phantom ${element.text}"
            val newTypeParameter = project.psiFactory.typeParameter(newText)
            element.replace(newTypeParameter)
        }
    }

    class Remove(typeParameter: MvTypeParameter) : PhantomFix(typeParameter) {

        override fun getFamilyName(): String = "Remove phantom"
        override fun getText(): String = "Remove phantom"

        override fun stillApplicable(project: Project, file: PsiFile, element: MvTypeParameter): Boolean =
            element.isPhantom

        override fun invoke(project: Project, file: PsiFile, element: MvTypeParameter) {
            val boundText = element.typeParamBound?.text ?: ""
            val newText = "${element.name}$boundText"
            val newTypeParameter = project.psiFactory.typeParameter(newText)
            element.replace(newTypeParameter)
        }
    }

}
