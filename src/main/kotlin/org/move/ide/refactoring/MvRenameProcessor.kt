package org.move.ide.refactoring

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.descendantOfTypeStrict
import org.move.lang.core.psi.ext.hasChild
import org.move.lang.core.psi.ext.isShorthand

class MvRenameProcessor: RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement) = element is MvNamedElement

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        val psiFactory = element.project.psiFactory
        if (element is MvBindingPat) {
            usages.forEach {
                val field =
                    it.element?.ancestorOrSelf<MvStructLitField>(MvModuleBlock::class.java) ?: return@forEach
                if (field.isShorthand) {
                    val newField = psiFactory.createStructLitField(field.referenceName, newName)
                    field.replace(newField)
                }
            }
        }

        val newRenameElement = if (element is MvBindingPat && element.parent is MvStructPatField) {
            val newPatField = psiFactory.createStructPatField(element.identifier.text, element.text)
            element.replace(newPatField).descendantOfTypeStrict<MvBindingPat>()!!
        } else element

        super.renameElement(newRenameElement, newName, usages, listener)
    }
}
