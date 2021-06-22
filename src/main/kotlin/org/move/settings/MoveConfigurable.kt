package org.move.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel

class MoveConfigurable(val project: Project) : BoundConfigurable("Move"),
                                               Configurable.NoScroll {

    private val settingsPanel = MoveProjectSettingsPanel(project)

    private val state: MoveProjectSettingsService.State = project.moveSettings.settingsState

    override fun createPanel(): DialogPanel {
        return panel {
            settingsPanel.attachTo(this)
            row {
                checkBox("Collapse specs by default", state::collapseSpecs)
            }
        }
    }

    override fun disposeUIResources() {
        val dovePath = settingsPanel.selectedExecutablePath()
        val collapseSpecs = this.state.collapseSpecs
        project.moveSettings.settingsState =
            MoveProjectSettingsService.State(dovePath, collapseSpecs)

        super.disposeUIResources()
        Disposer.dispose(settingsPanel)
    }
}
