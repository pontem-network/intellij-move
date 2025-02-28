package org.move.utils.ui

import com.intellij.execution.ExecutionBundle
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidationRequestor
import com.intellij.ui.EditorTextComponent
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.PsiErrorElementUtil
import com.intellij.util.text.nullize
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JLabel

fun Row.ulongTextField(range: ULongRange?): Cell<JBTextField> {
    val result = cell(JBTextField())
        .validationOnInput {
            val value = it.text.toULongOrNull()
            when {
                value == null -> error(UIBundle.message("please.enter.a.number"))
                range != null && value !in range -> error(
                    UIBundle.message(
                        "please.enter.a.number.from.0.to.1",
                        range.first,
                        range.last
                    )
                )
                else -> null
            }
        }
    result.component.putClientProperty("dsl.intText.range", range)
    return result
}

fun <T : EditorTextField> Cell<T>.bindText(getter: () -> String, setter: (String) -> Unit): Cell<T> {
    val prop = MutableProperty(getter, setter)
    return bind(EditorTextField::getText, EditorTextField::setText, prop)
}

fun <T : JLabel> Cell<T>.bindLabelText(getter: () -> String, setter: (String) -> Unit): Cell<T> {
    val prop = MutableProperty(getter, setter)
    return bind(JLabel::getText, JLabel::setText, prop)
}

fun <T : EditorTextField> Cell<T>.registerValidationRequestor(): Cell<T> {
    validationRequestor(WHEN_TEXT_FIELD_TEXT_CHANGED(component))
    return this
}

private val WHEN_TEXT_FIELD_TEXT_CHANGED =
    DialogValidationRequestor.WithParameter<EditorTextField> { textComponent ->
        DialogValidationRequestor { _, validate ->
            textComponent.whenDocumentChanged { validate() }
        }
    }

private fun EditorTextComponent.whenDocumentChanged(listener: (DocumentEvent) -> Unit) {
    this.addDocumentListener(object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            listener(event)
        }
    })
}

fun validateNonEmpty(message: String): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
    return {
        if (it.text.isNullOrEmpty()) error(message) else null
    }
}

fun validateEditorTextNonEmpty(message: String): ValidationInfoBuilder.(LanguageTextField) -> ValidationInfo? {
    return {
        if (it.text.isEmpty()) error(message) else null
    }
}

fun validateParseErrors(message: String): ValidationInfoBuilder.(LanguageTextField) -> ValidationInfo? {
    return {
        FileDocumentManager.getInstance().getFile(it.document)
            ?.let { file ->
                if (PsiErrorElementUtil.hasErrors(it.project, file)) {
                    error(message)
                } else null
            }
    }
}

fun LanguageTextField.hasParseErrors(): Boolean {
    return FileDocumentManager.getInstance().getFile(this.document)
        ?.let { file ->
            PsiErrorElementUtil.hasErrors(this.project, file)
//            if (PsiErrorElementUtil.hasErrors(field.project, file)) {
//                false
//            } else true
        } ?: false
}

class WorkingDirectoryField : LabeledComponent<TextFieldWithBrowseButton>() {
    init {
        component = TextFieldWithBrowseButton().apply {
            val fileChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                title = ExecutionBundle.message("select.working.directory.message")
            }
            addBrowseFolderListener(null, fileChooser)
        }
        text = ExecutionBundle.message("run.configuration.working.directory.label")
    }

    fun toPath(): Path? = this.component.text.nullize()?.let { Paths.get(it) }
}
