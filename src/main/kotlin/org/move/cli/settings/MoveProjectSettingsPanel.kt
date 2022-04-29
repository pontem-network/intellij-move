package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.layout.enteredTextSatisfies
import org.move.cli.AptosCLI
import org.move.openapiext.UiDebouncer
import org.move.openapiext.pathTextField
import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class VersionLabel : JLabel() {
    fun setVersion(version: String?) {
        if (version == null) {
            this.text = "N/A"
            this.foreground = JBColor.RED
        } else {
            // preformat version in case of multiline string
            this.text = version
                .split("\n")
                .joinToString("<br>", "<html>", "</html>")
            this.foreground = JBColor.foreground()
        }
    }
}

class FilePathWithVersionField(private val project: Project) : Disposable {
    override fun dispose() {}
    private val versionUpdateDebouncer = UiDebouncer(this)

    val field =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "CLI binary"
        ) { updateVersion() }
    val versionLabel = VersionLabel()

    init {
        field.textField.text =
            project.moveSettings.settingsState.aptosCliPath
    }

    fun selectedMoveBinaryPath(): String = this.field.textField.text

    val valid = field.textField.enteredTextSatisfies { it.isNotBlank() }

    private fun updateVersion() {
        this.updateVersionField(field, versionLabel)
    }

    private fun updateVersionField(
        executablePathField: TextFieldWithBrowseButton,
        versionLabel: VersionLabel
    ) {
        val binPath = executablePathField.text
        versionUpdateDebouncer.run(
            onPooledThread = {
                AptosCLI(project, Paths.get(binPath)).version()
            },
            onUiThread = { version -> versionLabel.setVersion(version) }
        )
    }
}

fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
