package org.move.cli.settings

import com.intellij.ui.JBColor
import java.awt.BorderLayout
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


fun wrapComponent(component: JComponent): JComponent =
    JPanel(BorderLayout()).apply {
        add(component, BorderLayout.NORTH)
    }
