package org.move.cli.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class PerProjectMoveConfigurable(val project: Project) : BoundConfigurable("Move Language"),
                                                         SearchableConfigurable {

    override fun getId(): String = "org.move.settings"

    private val settingsState: MoveProjectSettingsService.State = project.moveSettings.state

    private val aptosSettingsPanel = AptosSettingsPanel(showDefaultProjectSettingsLink = !project.isDefault)

    override fun createPanel(): DialogPanel {
        return panel {
            aptosSettingsPanel.attachTo(this)
            group {
                row {
                    checkBox("Auto-fold specs in opened files")
                        .bindSelected(settingsState::foldSpecs)
                }
                row {
                    checkBox("Disable telemetry for new Run Configurations")
                        .bindSelected(settingsState::disableTelemetry)
                }
                row {
                    checkBox("Enable debug mode")
                        .bindSelected(settingsState::debugMode)
                    comment(
                        "Enables some explicit crashes in the plugin code. Useful for the error reporting."
                    )
                }
                row {
                    checkBox("Skip fetching latest git dependencies for tests")
                        .bindSelected(settingsState::skipFetchLatestGitDeps)
                    comment(
                        "Adds --skip-fetch-latest-git-deps to the test runs."
                    )
                }
            }
        }
    }

    override fun disposeUIResources() {
        super<BoundConfigurable>.disposeUIResources()
        Disposer.dispose(aptosSettingsPanel)
    }

    override fun reset() {
        super<BoundConfigurable>.reset()
        aptosSettingsPanel.panelData =
            AptosSettingsPanel.PanelData(AptosExec.fromSettingsFormat(settingsState.aptosPath))
//        aptosSettingsPanel.panelData = AptosSettingsPanel.PanelData(state.aptosPath)
    }

    override fun isModified(): Boolean {
        if (super<BoundConfigurable>.isModified()) return true
        val panelData = aptosSettingsPanel.panelData
        return panelData.aptosExec.pathToSettingsFormat() != settingsState.aptosPath
    }

    override fun apply() {
        super.apply()
        val newSettingsState = settingsState
        newSettingsState.aptosPath = aptosSettingsPanel.panelData.aptosExec.pathToSettingsFormat()
        project.moveSettings.state = newSettingsState

    }
}
