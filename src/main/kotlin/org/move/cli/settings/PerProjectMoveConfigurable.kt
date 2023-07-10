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

    private val state: MoveProjectSettingsService.State = project.moveSettings.settingsState

    private val moveProjectSettings = MoveSettingsPanel(showDefaultSettingsLink = !project.isDefault)

    override fun createPanel(): DialogPanel {
        return panel {
            moveProjectSettings.attachTo(this)
            group {
                row {
                    checkBox("Auto-fold specs in opened files")
                        .bindSelected(state::foldSpecs)
                }
                row {
                    checkBox("Disable telemetry for new Run Configurations")
                        .bindSelected(state::disableTelemetry)
                }
                row {
                    checkBox("Enable debug mode")
                        .bindSelected(state::debugMode)
                    comment(
                        "Enables some explicit crashes in the plugin code. Useful for the error reporting."
                    )
                }
                row {
                    checkBox("Skip fetching latest git dependencies for tests")
                        .bindSelected(state::skipFetchLatestGitDeps)
                    comment(
                        "Adds --skip-fetch-latest-git-deps to the test runs."
                    )
                }
            }
        }
    }

    override fun disposeUIResources() {
        super<BoundConfigurable>.disposeUIResources()
        Disposer.dispose(moveProjectSettings)
    }

    override fun reset() {
        super<BoundConfigurable>.reset()
        moveProjectSettings.data = MoveSettingsPanel.Data(state.aptosPath)
    }

    override fun isModified(): Boolean {
        if (super<BoundConfigurable>.isModified()) return true
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
