package org.move.cli

import com.intellij.execution.ExecutionBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EditorTextField
import com.intellij.ui.TextAccessor
import com.intellij.ui.layout.panel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class DoveExecutableSettingsEditor : SettingsEditor<DoveRunConfiguration>() {
    private val textField = EditorTextField()

    override fun resetEditorFrom(configuration: DoveRunConfiguration) {
        textField.text = configuration.command
    }

    override fun applyEditorTo(configuration: DoveRunConfiguration) {
        configuration.command = textField.text
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Command:") {
                textField(growX, pushX)
            }
        }
    }
}

private class WorkingDirectoryComponent : LabeledComponent<TextFieldWithBrowseButton>() {
    init {
        component = TextFieldWithBrowseButton().apply {
            val fileChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                title = ExecutionBundle.message("select.working.directory.message")
            }
            addBrowseFolderListener(null, null, null, fileChooser)
        }
        text = ExecutionBundle.message("run.configuration.working.directory.label")
    }
}

private class CommandLineEditor : JPanel(BorderLayout()), TextAccessor {
    private val textField = EditorTextField("")

    init {
        add(textField, BorderLayout.CENTER)
    }

    override fun setText(text: String?) {
        textField.setText(text)
    }

    override fun getText(): String = textField.text
}















