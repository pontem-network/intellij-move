package org.move.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.layout.LayoutBuilder
import org.move.cli.VersionedExecutable
import org.move.openapiext.UiDebouncer
import org.move.openapiext.pathTextField
import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class VersionLabel: JLabel() {
    fun setVersion(version: String?) {
        if (version == null) {
            this.text = "N/A"
            this.foreground = JBColor.RED
        } else {
            this.text = version
            this.foreground = JBColor.foreground()
        }
    }
}

class MoveProjectSettingsPanel(private val project: Project) : Disposable {
    override fun dispose() {}
    private val versionUpdateDebouncer = UiDebouncer(this)

    private val doveExecutablePathField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Dove executable"
        ) { updateDoveVersion() }
    private val doveVersion = VersionLabel()

    private val moveCLIExecutablePathField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Move CLI executable"
        ) { updateMoveCLIVersion() }
    private val moveCliVersion = VersionLabel()

    init {
        doveExecutablePathField.textField.text =
            project.moveSettings.settingsState.doveExecutablePath
        moveCLIExecutablePathField.textField.text =
            project.moveSettings.settingsState.moveCliExecutablePath
    }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        titledRow("Dove") {
            subRowIndent = 0
            row("Dove executable:") { wrapComponent(doveExecutablePathField)(growX, pushX) }
            row("Dove version") { doveVersion() }
        }
        titledRow("Move CLI") {
            subRowIndent = 0
            row("Move CLI executable:") { wrapComponent(moveCLIExecutablePathField)(growX, pushX) }
            row("Move CLI version") { moveCliVersion() }
        }
    }

    fun selectedDovePath(): String = this.doveExecutablePathField.textField.text

    fun selectedMoveCLIPath(): String = this.moveCLIExecutablePathField.textField.text

    private fun updateDoveVersion() {
        this.updateVersionField(doveExecutablePathField, doveVersion)
    }
    private fun updateMoveCLIVersion() {
        this.updateVersionField(moveCLIExecutablePathField, moveCliVersion)
    }

    private fun updateVersionField(executablePathField: TextFieldWithBrowseButton,
                                   versionLabel: VersionLabel) {
        val executablePath = executablePathField.text
        versionUpdateDebouncer.run(
            onPooledThread = {
                VersionedExecutable(project, Paths.get(executablePath)).version()
            },
            onUiThread = { version -> versionLabel.setVersion(version)}
        )
    }
}

private fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
