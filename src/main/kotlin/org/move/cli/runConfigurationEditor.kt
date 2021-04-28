package org.move.cli

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EditorTextField
import com.intellij.ui.TextAccessor
import com.intellij.ui.layout.panel
import com.intellij.util.text.nullize
import java.awt.BorderLayout
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JPanel

class DoveExecutableSettingsEditor : SettingsEditor<DoveRunConfiguration>() {
    private val textField = EditorTextField()

    val currentWorkingDirectory: Path?
        get() = workingDirectory.component.text.nullize()?.let { Paths.get(it) }

    val workingDirectory: LabeledComponent<TextFieldWithBrowseButton> =
        WorkingDirectoryComponent()

//    val environmentVariables = EnvironmentVariablesComponent()

    override fun resetEditorFrom(configuration: DoveRunConfiguration) {
        textField.text = configuration.command
        workingDirectory.component.text = configuration.workingDirectory?.toString().orEmpty()
    }

    override fun applyEditorTo(configuration: DoveRunConfiguration) {
        configuration.command = textField.text
        configuration.workingDirectory = currentWorkingDirectory
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Command:") {
                textField(growX, pushX)
            }
//            row(environmentVariables.label) { environmentVariables }
            row(workingDirectory.label) {
                workingDirectory(growX)
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















