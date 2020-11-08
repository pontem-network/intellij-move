package org.move.movec.project.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.ui.layout.LayoutBuilder
import org.move.openapiext.pathTextField
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MoveProjectSettingsPanel : Disposable {
    override fun dispose() {}

    private val pathToMovecField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Select movec binary")

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row("Movec binary location:") { wrapComponent(pathToMovecField)(growX, pushX) }
    }
}

private fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
