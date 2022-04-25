package org.move.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel

class MoveConfigurable(val project: Project) : BoundConfigurable("Move Language"),
                                               Configurable.NoScroll {

    private val moveBinaryPathPanel = MoveBinaryPathPanel(project)

    private val state: MvProjectSettingsService.State = project.moveSettings.settingsState

    override fun createPanel(): DialogPanel {
        return panel {
            moveBinaryPathPanel.attachTo(this)
            row {
                buttonGroup("Project type") {
                    row { radioButton("Aptos", state::isAptos) }
                    row { radioButton("Dove", state::isDove) }
                }
            }
            row {
                checkBox("Collapse specs by default", state::collapseSpecs)
            }
        }
    }

    override fun disposeUIResources() {
        val isAptos = this.state.isAptos
        val isDove = this.state.isDove
        val moveExecutablePath = moveBinaryPathPanel.selectedMoveBinaryPath()
        val collapseSpecs = this.state.collapseSpecs
        project.moveSettings.settingsState = MvProjectSettingsService.State(
            isAptos,
            isDove,
            moveExecutablePath,
            collapseSpecs
        )
        super.disposeUIResources()
        Disposer.dispose(moveBinaryPathPanel)
    }
}
