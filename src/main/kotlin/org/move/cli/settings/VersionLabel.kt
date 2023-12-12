package org.move.cli.settings

import com.intellij.ui.JBColor
import javax.swing.JLabel

class VersionLabel : JLabel() {
    fun setText(text: String, errorHighlighting: Boolean) {
        if (errorHighlighting) {
            this.text = text
            this.foreground = JBColor.RED
        } else {
            this.text = text
                .split("\n")
                .joinToString("<br>", "<html>", "</html>")
            this.foreground = JBColor.foreground()
        }
    }

    fun setLabelText(version: String?, error: String?) {
        if (version == null) {
            var text = "N/A"
            if (error != null) {
                text += " ($error)"
            }
            this.text = text
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
