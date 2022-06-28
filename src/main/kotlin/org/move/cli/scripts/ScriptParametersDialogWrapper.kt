package org.move.cli.scripts

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.parameterBindings
import org.move.lang.core.psi.parameters
import javax.swing.JComponent

class RunScriptDialog(val scriptFunction: MvFunction) : DialogWrapper(scriptFunction.project) {

    override fun createCenterPanel(): JComponent = panel {
        for (binding in scriptFunction.parameterBindings) {
            row(binding.name ?: "") { label(binding.name ?: "") }
        }
    }
}
