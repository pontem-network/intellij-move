package org.move.project.settings.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.ui.layout.LayoutBuilder
import org.move.openapiext.pathTextField
import java.awt.BorderLayout
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

class MoveProjectSettingsPanel(executablePath: String) : Disposable {
    override fun dispose() {}

    private val pathToDoveExecutableField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Dove executable"
        )

    init {
        pathToDoveExecutableField.textField.text = executablePath
    }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row("Dove executable:") { wrapComponent(pathToDoveExecutableField)(growX, pushX) }
    }

    fun getExecutablePath(): String {
        return pathToDoveExecutableField.text
    }
}

private fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
