package org.move.cli.runconfig

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EditorTextField
import com.intellij.ui.layout.panel
import com.intellij.util.text.nullize
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent

class MoveRunConfigurationEditor : SettingsEditor<MoveRunConfiguration>() {
    private val textField = EditorTextField()
    private val environmentVariables = EnvironmentVariablesComponent()

    val currentWorkingDirectory: Path?
        get() = workingDirectory.component.text.nullize()?.let { Paths.get(it) }

    val workingDirectory: LabeledComponent<TextFieldWithBrowseButton> =
        WorkingDirectoryComponent()

    override fun resetEditorFrom(configuration: MoveRunConfiguration) {
        textField.text = configuration.cmd.command
        workingDirectory.component.text = configuration.cmd.workingDirectory?.toString().orEmpty()
        environmentVariables.envData = configuration.env
    }

    override fun applyEditorTo(configuration: MoveRunConfiguration) {
        val command = textField.text
        configuration.cmd = MoveCommandLine(command, this.currentWorkingDirectory)
        configuration.env = environmentVariables.envData
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Command:") {
                textField(growX, pushX)
            }
            row(environmentVariables.label) {
                environmentVariables(growX)
            }
            row(workingDirectory.label) {
                workingDirectory(growX)
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
}
