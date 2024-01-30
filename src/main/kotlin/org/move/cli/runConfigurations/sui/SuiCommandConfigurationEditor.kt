package org.move.cli.runConfigurations.sui

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.move.utils.ui.WorkingDirectoryField
import java.nio.file.Path
import javax.swing.JComponent

class SuiCommandConfigurationEditor : SettingsEditor<SuiCommandConfiguration>() {
    private val commandTextField = JBTextField()
    private val envVarsField = EnvironmentVariablesComponent()
    val workingDirectoryField = WorkingDirectoryField()

    private val workingDirectory: Path? get() = workingDirectoryField.toPath()

    override fun resetEditorFrom(configuration: SuiCommandConfiguration) {
        commandTextField.text = configuration.command
        workingDirectoryField.component.text = configuration.workingDirectory?.toString().orEmpty()
        envVarsField.envData = configuration.environmentVariables
    }

    override fun applyEditorTo(configuration: SuiCommandConfiguration) {
        configuration.command = commandTextField.text
        configuration.workingDirectory = this.workingDirectory
        configuration.environmentVariables = envVarsField.envData
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Command:") {
                cell(commandTextField)
                    .align(AlignX.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
                    .columns(COLUMNS_LARGE)
            }
            row(envVarsField.label) {
                cell(envVarsField)
                    .align(AlignX.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
            }
            row(workingDirectoryField.label) {
                cell(workingDirectoryField)
                    .align(AlignX.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
    }
}
