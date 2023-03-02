package org.move.utils.ui

import com.intellij.codeInsight.AutoPopupController
import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.LanguageTextField
import com.intellij.util.textCompletion.TextCompletionProvider
import com.intellij.util.textCompletion.TextCompletionUtil.DocumentWithCompletionCreator
import org.move.lang.MoveLanguage
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvQualPathTypeCodeFragment

class CodeFragmentDocumentCreator(
    val project: Project,
    completionProvider: TextCompletionProvider,
    val fragmentContext: MvElement,
    val text: String,
) :
    DocumentWithCompletionCreator(completionProvider, true, false) {

    override fun createDocument(_value: String?, _language: Language?, _project: Project?): Document {
        val codeFragment = MvQualPathTypeCodeFragment(
            project,
            text,
            fragmentContext
        )
        this.customizePsiFile(codeFragment)
        val document = PsiDocumentManager.getInstance(project).getDocument(codeFragment)!!
        return document
    }
}

class MoveTextFieldWithCompletion(
    project: Project,
    text: String,
    completionProvider: TextCompletionProvider,
    fragmentContext: MvElement,
) :
    LanguageTextField(
        MoveLanguage,
        project,
        text,
        CodeFragmentDocumentCreator(project, completionProvider, fragmentContext, text),
        true
    ) {

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()

        val disableSpellChecking = SpellCheckingEditorCustomizationProvider.getInstance().disabledCustomization
        if (disableSpellChecking != null) {
            disableSpellChecking.customize(editor)
        }
        editor.putUserData(AutoPopupController.ALWAYS_AUTO_POPUP, true)

//        if (myShowHint) {
//            TextCompletionUtil.installCompletionHint(editor)
//        }

        return editor
    }

}
