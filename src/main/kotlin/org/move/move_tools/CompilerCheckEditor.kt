package org.move.move_tools

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.EditorTextField
import com.intellij.ui.layout.panel
import javax.swing.JComponent


class CompilerCheckEditor : SettingsEditor<CompilerCheckConfiguration>() {
    private var senderAddress = EditorTextField("")
//    private var stdlibFolder: String = ""

    override fun createEditor(): JComponent = panel {
        row {
            label("Sender:")
            senderAddress()
        }
//        row {
//            label("Standard library:")
//            textField({ stdlibFolder }, { stdlibFolder = it })
//        }
    }
//    private fun LayoutBuilder.labeledRow(labelText: String, component: JComponent, init: Row.() -> Unit) {
//        val label = Label(labelText)
//        label.labelFor = component
//        row(label) { init() }
//    }

    override fun resetEditorFrom(s: CompilerCheckConfiguration) {
        senderAddress.text = s.senderAddress
    }

    override fun applyEditorTo(s: CompilerCheckConfiguration) {
        s.senderAddress = senderAddress.text
    }
}
