package org.move.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.layout.LayoutBuilder
import org.move.cli.DoveExecutable
import org.move.openapiext.UiDebouncer
import org.move.openapiext.pathTextField
import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class MoveProjectSettingsPanel(private val project: Project) : Disposable {
    override fun dispose() {}

    private val versionUpdateDebouncer = UiDebouncer(this)

    private val executablePathField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Dove executable"
        ) { update() }
    private val doveVersion = JLabel()

    init {
        executablePathField.textField.text = project.moveSettings.settingsState.doveExecutablePath
    }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row("Dove executable:") { wrapComponent(executablePathField)(growX, pushX) }
        row("Dove version") { doveVersion() }
    }

    fun selectedExecutablePath(): String = this.executablePathField.textField.text

    private fun update() {
        val pathToExecutable = executablePathField.text
        versionUpdateDebouncer.run(
            onPooledThread = {
                val dove = DoveExecutable(project, Paths.get(pathToExecutable))
                val version = dove.version()
                version
            },
            onUiThread = { version ->
                if (version == null) {
                    doveVersion.text = "N/A"
                    doveVersion.foreground = JBColor.RED
                } else {
                    doveVersion.text = version
                    doveVersion.foreground = JBColor.foreground()
                }
//                updateListener?.invoke()
            }
        )
    }
}

private fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
