package org.move.cli.util

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.ExpandableEditorSupport
import com.intellij.ui.TextAccessor
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import java.awt.BorderLayout
import javax.swing.JPanel

class RsCommandLineEditor(
    private val project: Project,
    private val completionProvider: TextFieldCompletionProvider
): JPanel(BorderLayout()), TextAccessor {

    private val textField = createTextField("")

    init {
        ExpandableEditorSupportWithCustomPopup(textField, this::createTextField)
        add(textField, BorderLayout.CENTER)
    }

    override fun setText(text: String?) {
        textField.setText(text)
    }

    override fun getText(): String = textField.text

    private fun createTextField(value: String): TextFieldWithCompletion =
        TextFieldWithCompletion(
            project,
            completionProvider,
            value,
            true,
            false,
            false
        )

    class EmptyTextFieldCompletionProvider: TextFieldCompletionProvider() {
        override fun addCompletionVariants(
            text: String,
            offset: Int,
            prefix: String,
            result: CompletionResultSet
        ) {
        }
    }
}

private class ExpandableEditorSupportWithCustomPopup(
    field: EditorTextField,
    private val createPopup: (text: String) -> EditorTextField
): ExpandableEditorSupport(field) {

    override fun createPopupEditor(field: EditorTextField, text: String): EditorTextField = createPopup(text)
}
