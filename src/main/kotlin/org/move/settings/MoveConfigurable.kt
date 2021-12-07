package org.move.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel

class MvConfigurable(val project: Project) : BoundConfigurable("Move"),
                                               Configurable.NoScroll {

    private val settingsPanel = MvProjectSettingsPanel(project)

    private val state: MvProjectSettingsService.State = project.moveSettings.settingsState

    override fun createPanel(): DialogPanel {
        return panel {
            settingsPanel.attachTo(this)
            row {
                checkBox("Collapse specs by default", state::collapseSpecs)
            }
        }
    }

    override fun disposeUIResources() {
        val moveExecutablePath = settingsPanel.selectedMvExecPath()
        val collapseSpecs = this.state.collapseSpecs
        project.moveSettings.settingsState =
            MvProjectSettingsService.State(moveExecutablePath, collapseSpecs)

        super.disposeUIResources()
        Disposer.dispose(settingsPanel)
    }
}
