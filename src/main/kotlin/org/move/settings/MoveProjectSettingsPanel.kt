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

    private val moveExecutablePathField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Move CLI executable"
        ) { updateMoveCLIVersion() }
    private val moveExecVersion = VersionLabel()

    init {
        moveExecutablePathField.textField.text =
            project.moveSettings.settingsState.moveExecutablePath
    }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row("Move executable:") { wrapComponent(moveExecutablePathField)(growX, pushX) }
        row("Move executable version") { moveExecVersion() }
    }

    fun selectedMoveExecPath(): String = this.moveExecutablePathField.textField.text

    private fun updateMoveCLIVersion() {
        this.updateVersionField(moveExecutablePathField, moveExecVersion)
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
