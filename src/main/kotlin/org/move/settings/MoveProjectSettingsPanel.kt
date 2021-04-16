package org.move.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.ui.layout.LayoutBuilder
import org.move.openapiext.pathTextField
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MoveProjectSettingsPanel(doveExecutablePath: String) : Disposable {
    override fun dispose() {}

    private val executablePathField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Dove executable"
        )

    init {
        executablePathField.textField.text = doveExecutablePath
    }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row("Dove executable:") { wrapComponent(executablePathField)(growX, pushX) }
    }

    fun selectedExecutablePath(): String = this.executablePathField.textField.text
}

private fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
