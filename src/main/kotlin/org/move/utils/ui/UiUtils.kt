package org.move.utils.ui

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.ui.validation.DialogValidationRequestor
import com.intellij.ui.EditorTextComponent
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty

fun <T : LanguageTextField> Cell<T>.bindText(getter: () -> String, setter: (String) -> Unit): Cell<T> {
    val prop = MutableProperty(getter, setter)
    return bind(LanguageTextField::getText, LanguageTextField::setText, prop)
}

fun <T : LanguageTextField> Cell<T>.registerValidationRequestor(): Cell<T> {
    validationRequestor(WHEN_TEXT_FIELD_TEXT_CHANGED(component))
    return this
}

private val WHEN_TEXT_FIELD_TEXT_CHANGED = DialogValidationRequestor.WithParameter<EditorTextField> { textComponent ->
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
