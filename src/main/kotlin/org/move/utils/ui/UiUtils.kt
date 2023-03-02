package org.move.utils.ui

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.ui.validation.DialogValidationRequestor
import com.intellij.ui.EditorTextComponent
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Row

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

fun <T : LanguageTextField> Cell<T>.bindText(getter: () -> String, setter: (String) -> Unit): Cell<T> {
    val prop = MutableProperty(getter, setter)
    return bind(LanguageTextField::getText, LanguageTextField::setText, prop)
}

fun <T : LanguageTextField> Cell<T>.registerValidationRequestor(): Cell<T> {
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
