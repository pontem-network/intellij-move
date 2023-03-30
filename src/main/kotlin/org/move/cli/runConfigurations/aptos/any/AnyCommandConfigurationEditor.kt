package org.move.cli.runConfigurations.aptos.any

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.EditorTextField
import com.intellij.ui.layout.panel
import org.move.utils.ui.WorkingDirectoryField
import java.nio.file.Path
import javax.swing.JComponent

class AnyCommandConfigurationEditor : SettingsEditor<AnyCommandConfiguration>() {
    private val commandTextField = EditorTextField()
    private val envVarsField = EnvironmentVariablesComponent()
    val workingDirectoryField = WorkingDirectoryField()

    private val workingDirectory: Path? get() = workingDirectoryField.toPath()

    override fun resetEditorFrom(configuration: AnyCommandConfiguration) {
        commandTextField.text = configuration.command
        workingDirectoryField.component.text = configuration.workingDirectory?.toString().orEmpty()
        envVarsField.envData = configuration.environmentVariables
    }

    override fun applyEditorTo(configuration: AnyCommandConfiguration) {
        configuration.command = commandTextField.text
        configuration.workingDirectory = this.workingDirectory
        configuration.environmentVariables = envVarsField.envData
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Command:") {
                commandTextField(growX, pushX)
            }
            row(envVarsField.label) {
                envVarsField(growX)
            }
            row(workingDirectoryField.label) {
                workingDirectoryField(growX)
            }
        }
    }
}
