package org.move.ide.refactoring

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.intellij.usageView.UsageInfo
import org.move.lang.MoveFile
import org.move.lang.core.psi.rename
import org.move.lang.modules

class MvRenameFileProcessor : RenamePsiFileProcessor() {
    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        val moveFile = element as? MoveFile ?: return
        val oldName = moveFile.virtualFile.nameWithoutExtension
        val module = moveFile.modules().singleOrNull() ?: return
        if (module.name == oldName) {
            module.rename(newName)
        }
        super.renameElement(element, newName, usages, listener)
    }
}
