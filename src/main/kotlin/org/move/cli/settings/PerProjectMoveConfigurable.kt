package org.move.cli.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import org.move.cli.settings.aptos.ChooseAptosCliPanel

class PerProjectMoveConfigurable(val project: Project) : BoundConfigurable("Move Language"),
                                                         SearchableConfigurable {

    override fun getId(): String = "org.move.settings"

    private val settingsState: MoveProjectSettingsService.State = project.moveSettings.state

    private val chooseAptosCliPanel = ChooseAptosCliPanel(showDefaultProjectSettingsLink = !project.isDefault)

    override fun createPanel(): DialogPanel {
        return panel {
//            chooseAptosCliPanel.setSelectedAptosExec(AptosExec.default())
            chooseAptosCliPanel.attachToLayout(this)
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
                row {
                    checkBox("Dump storage to console on test failures")
                        .bindSelected(settingsState::dumpStateOnTestFailure)
                    comment(
                        "Adds --dump to the test runs."
                    )
                }
            }
        }
    }

    override fun disposeUIResources() {
        super<BoundConfigurable>.disposeUIResources()
        Disposer.dispose(chooseAptosCliPanel)
    }

    /// loads settings from configurable to swing form
    override fun reset() {
        chooseAptosCliPanel.selectedAptosExec = settingsState.aptosExec()
        // should be invoked at the end
        super<BoundConfigurable>.reset()
    }

    /// checks whether any settings are modified (should be fast)
    override fun isModified(): Boolean {
        if (super<BoundConfigurable>.isModified()) return true
        val selectedAptosExec = chooseAptosCliPanel.selectedAptosExec
        return selectedAptosExec != settingsState.aptosExec()
    }

    /// saves values from Swing form back to configurable (OK / Apply)
    override fun apply() {
        super.apply()
        project.moveSettings.state =
            settingsState.copy(aptosPath = chooseAptosCliPanel.selectedAptosExec.pathToSettingsFormat())

    }
}
