package org.move.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.layout.LayoutBuilder
import org.move.cli.MoveBinary
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
            // preformat version in case of multiline string
            this.text = version
                .split("\n")
                .joinToString("<br>", "<html>", "</html>")
            this.foreground = JBColor.foreground()
        }
    }
}

class MoveBinaryPathPanel(private val project: Project) : Disposable {
    override fun dispose() {}
    private val versionUpdateDebouncer = UiDebouncer(this)

    private val moveBinaryPathField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Move CLI binary"
        ) { updateMoveBinaryVersion() }
    private val moveBinaryVersion = VersionLabel()

    init {
        moveBinaryPathField.textField.text =
            project.moveSettings.settingsState.moveExecutablePath
    }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row("Move binary:") { wrapComponent(moveBinaryPathField)(growX, pushX) }
        row("Move version") { moveBinaryVersion() }
    }

    fun selectedMoveBinaryPath(): String = this.moveBinaryPathField.textField.text

    private fun updateMoveBinaryVersion() {
        this.updateVersionField(moveBinaryPathField, moveBinaryVersion)
    }

    private fun updateVersionField(executablePathField: TextFieldWithBrowseButton,
                                   versionLabel: VersionLabel) {
        val binPath = executablePathField.text
        versionUpdateDebouncer.run(
            onPooledThread = {
                MoveBinary(project, Paths.get(binPath)).version()
            },
            onUiThread = { version -> versionLabel.setVersion(version)}
        )
    }
}

private fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
