package org.move.cli.sdks

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.move.cli.runConfigurations.aptos.APTOS_VERSION_REGEX
import org.move.openapiext.pathField
import javax.swing.JComponent

class DownloadAptosSdkDialog(val project: Project?): DialogWrapper(project, true) {

    val versionField = JBTextField()
    val targetSdksDirField = pathField(
        FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        this.disposable,
        "Choose Target Dir"
    )

    init {
        title = "Select Aptos SDK"

        targetSdksDirField.text = sdksService().sdksDir ?: ""

        init()
    }

    override fun createCenterPanel(): JComponent {
        val settings = sdksService()
        return panel {
            row("Aptos CLI version:") {
                cell(versionField)
                    .align(AlignX.FILL)
                    .columns(10)
                    .validationOnApply { field ->
                        if (!field.text.matches(APTOS_VERSION_REGEX)) {
                            ValidationInfo("Version is invalid. Should be in form of MAJOR.MINOR.PATCH")
                        } else {
                            null
                        }
                    }
            }
            row("Target directory:") {
                cell(targetSdksDirField)
                    .align(AlignX.FILL)
                    .columns(16)
            }

            onApply {
                settings.state.sdksDir = targetSdksDirField.text
            }
        }
    }
}
