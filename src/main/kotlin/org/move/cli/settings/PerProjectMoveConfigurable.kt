package org.move.cli.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import org.move.cli.settings.aptos.ChooseAptosCliPanel
import org.move.cli.settings.sui.ChooseSuiCliPanel
import org.move.openapiext.showSettings

class PerProjectMoveConfigurable(val project: Project):
    BoundSearchableConfigurable(
        displayName = "Move Language",
        helpTopic = "Move_language_settings",
        _id = "org.move.settings"
    ) {

    private val settingsState: MoveProjectSettingsService.State = project.moveSettings.state

    private val chooseAptosCliPanel = ChooseAptosCliPanel()
    private val chooseSuiCliPanel = ChooseSuiCliPanel()

    override fun createPanel(): DialogPanel {
        return panel {
            group {
                var aptosRadioButton: Cell<JBRadioButton>? = null
                var suiRadioButton: Cell<JBRadioButton>? = null
                buttonsGroup("Blockchain") {
                    row {
                        aptosRadioButton = radioButton("Aptos")
                            .bindSelected(
                                { settingsState.blockchain == Blockchain.APTOS },
                                { settingsState.blockchain = Blockchain.APTOS },
                            )
                        suiRadioButton = radioButton("Sui")
                            .bindSelected(
                                { settingsState.blockchain == Blockchain.SUI },
                                { settingsState.blockchain = Blockchain.SUI },
                            )
                    }
                }
                chooseAptosCliPanel.attachToLayout(this)
                    .visibleIf(aptosRadioButton!!.selected)
                chooseSuiCliPanel.attachToLayout(this)
                    .visibleIf(suiRadioButton!!.selected)
            }
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
                        "Adds --dump to the test runs (aptos only)."
                    )
                }
            }
            if (!project.isDefault) {
                row {
                    link("Set default project settings") {
                        ProjectManager.getInstance().defaultProject.showSettings<PerProjectMoveConfigurable>()
                    }
//                        .visible(true)
                        .align(AlignX.RIGHT)
                    //                .horizontalAlign(HorizontalAlign.RIGHT)
                }
            }
        }
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        Disposer.dispose(chooseAptosCliPanel)
        Disposer.dispose(chooseSuiCliPanel)
    }

    /// checks whether any settings are modified (should be fast)
    override fun isModified(): Boolean {
        // checks whether panel created in the createPanel() is modified, defined in DslConfigurableBase
        if (super.isModified()) return true
        val selectedAptosExec = chooseAptosCliPanel.selectedAptosExec
        val selectedSui = chooseSuiCliPanel.getSuiCliPath()
        return selectedAptosExec != settingsState.aptosExec()
                || selectedSui != settingsState.suiPath
    }

    /// loads settings from configurable to swing form
    override fun reset() {
        chooseAptosCliPanel.selectedAptosExec = settingsState.aptosExec()
        chooseSuiCliPanel.setSuiCliPath(settingsState.suiPath)
        // resets panel created in createPanel(), see DslConfigurableBase
        // should be invoked at the end
        super.reset()
    }

    /// saves values from Swing form back to configurable (OK / Apply)
    override fun apply() {
        // calls apply() for createPanel().value
        super.apply()
        project.moveSettings.state =
            settingsState.copy(aptosPath = chooseAptosCliPanel.selectedAptosExec.pathToSettingsFormat())
        project.moveSettings.state =
            settingsState.copy(suiPath = chooseSuiCliPanel.getSuiCliPath())

    }
}
