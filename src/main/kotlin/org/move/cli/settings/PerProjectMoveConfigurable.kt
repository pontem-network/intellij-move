package org.move.cli.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel

class PerProjectMoveConfigurable(val project: Project) : BoundConfigurable("Move Language"),
                                                         Configurable.NoScroll {

    private val state: MoveProjectSettingsService.State = project.moveSettings.settingsState

    private val moveProjectSettings = AptosSettingsPanel()

    override fun createPanel(): DialogPanel {
        return panel {
            moveProjectSettings.attachTo(this)
            titledRow("") {
                row { checkBox("Automatically fold specs in opened files", state::foldSpecs) }
            }
        }
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        Disposer.dispose(moveProjectSettings)
    }

    override fun reset() {
        super.reset()
        moveProjectSettings.data = AptosSettingsPanel.Data(
            aptosPath = state.aptosPath,
        )
    }

    override fun isModified(): Boolean {
        if (super.isModified()) return true
        val data = moveProjectSettings.data
        return data.aptosPath != state.aptosPath
    }

    override fun apply() {
        super.apply()
        val newState = state
        newState.aptosPath = moveProjectSettings.data.aptosPath
        project.moveSettings.settingsState = newState

    }
}
