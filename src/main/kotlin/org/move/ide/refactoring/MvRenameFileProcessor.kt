package org.move.ide.refactoring

import com.intellij.lang.LangBundle
import com.intellij.lang.LanguageNamesValidation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.intellij.usageView.UsageInfo
import org.move.lang.MoveFile
import org.move.lang.MoveLanguage
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.rename

class MvRenameFileProcessor : RenamePsiFileProcessor() {
    override fun createRenameDialog(
        project: Project,
        element: PsiElement,
        nameSuggestionContext: PsiElement?,
        editor: Editor?
    ): RenameDialog {
        return object : PsiFileRenameDialog(project, element, nameSuggestionContext, editor) {
            override fun canRun() {
                super.canRun()
                val moveFile = element as? MoveFile ?: return
                if (moveFile.singleModule() != null) {
                    val newModuleName = FileUtil.getNameWithoutExtension(newName)
                    val namesValidator = LanguageNamesValidation.INSTANCE.forLanguage(MoveLanguage)
                    if (namesValidator.isKeyword(newModuleName, moveFile.project)
                        || !namesValidator.isIdentifier(newModuleName, moveFile.project)
                    ) {
                        throw ConfigurationException(
                            LangBundle.message(
                                "dialog.message.valid.identifier",
                                newName
                            )
                        )
                    }
                }
            }
        }
    }

    override fun canProcessElement(element: PsiElement): Boolean {
        return super.canProcessElement(element) && element.language == MoveLanguage
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        val moveFile = element as? MoveFile ?: return

        val newModuleName = FileUtil.getNameWithoutExtension(newName)
        val module = moveFile.singleModule()
        if (module != null) {
            module.rename(newModuleName)
        }
        super.renameElement(element, newName, usages, listener)
    }

    private fun MoveFile.singleModule(): MvModule? {
        return modules()
            .singleOrNull()
            ?.takeIf { it.name == virtualFile.nameWithoutExtension }
    }
}
