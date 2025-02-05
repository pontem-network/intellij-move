package org.move.openapiext

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.util.Alarm
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class UiDebouncer(
    private val parentDisposable: CheckedDisposable,
    private val delayMillis: Int = 200
) {
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable)

    /**
     * @param onUiThread: callback to be executed in EDT with **any** modality state.
     * Use it only for UI updates
     */
    fun <T> update(onPooledThread: () -> T, onUiThread: (T) -> Unit) {
        if (parentDisposable.isDisposed) return
        alarm.cancelAllRequests()
        alarm.addRequest({
                             val r = onPooledThread()
                             invokeLater(ModalityState.any()) {
                                 if (!parentDisposable.isDisposed) {
                                     onUiThread(r)
                                 }
                             }
                         }, delayMillis)
    }
}

fun pathField(
    fileChooserDescriptor: FileChooserDescriptor,
    parentDisposable: Disposable,
    @DialogTitle dialogTitle: String,
    onTextChanged: (String) -> Unit = {}
): TextFieldWithBrowseButton {
    val component = TextFieldWithBrowseButton(null, parentDisposable)
    component.addBrowseFolderListener(
        null,
        fileChooserDescriptor,
        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
    )
    component.childComponent.addTextChangeListener {
        val documentText = it.document.getText(0, it.document.length)
        onTextChanged(documentText)
    }
    return component
}

fun JTextField.addTextChangeListener(listener: (DocumentEvent) -> Unit) {
    document.addDocumentListener(
        object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listener(e)
            }
        }
    )
}

fun <T: JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component).align(AlignX.FILL)
}
