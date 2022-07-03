package org.move.cli.scripts

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import org.move.lang.core.psi.MvFunction
import javax.swing.JComponent

class RunScriptDialog(val scriptFunction: MvFunction) : DialogWrapper(scriptFunction.project) {
    override fun createCenterPanel(): JComponent = panel {}
}
