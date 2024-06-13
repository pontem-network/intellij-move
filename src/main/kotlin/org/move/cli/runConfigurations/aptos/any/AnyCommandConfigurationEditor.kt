package org.move.cli.runConfigurations.aptos.any

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.move.openapiext.fullWidthCell
import org.move.utils.ui.WorkingDirectoryField
import java.nio.file.Path
import javax.swing.JComponent

class AnyCommandConfigurationEditor : SettingsEditor<AnyCommandConfiguration>() {
    private val commandTextField = ExpandableTextField()
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
            row("&Command:") {
                fullWidthCell(commandTextField)
//                    .columns(COLUMNS_LARGE)
                    .resizableColumn()
            }
            row(envVarsField.label) {
                fullWidthCell(envVarsField)
            }
            row(workingDirectoryField.label) {
                fullWidthCell(workingDirectoryField)
                    .resizableColumn()
            }
        }
    }
}
