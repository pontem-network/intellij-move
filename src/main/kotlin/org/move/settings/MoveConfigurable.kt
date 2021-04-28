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

    override fun createPanel(): DialogPanel {
        return panel {
            settingsPanel.attachTo(this)
        }
    }

    override fun disposeUIResources() {
        project.moveSettings.settingsState =
            MoveProjectSettingsService.State(settingsPanel.selectedExecutablePath())

        super.disposeUIResources()
        Disposer.dispose(settingsPanel)
    }
}
