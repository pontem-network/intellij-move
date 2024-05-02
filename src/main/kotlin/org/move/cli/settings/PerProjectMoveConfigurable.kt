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
import org.move.openapiext.showSettingsDialog

class PerProjectMoveConfigurable(val project: Project):
    BoundSearchableConfigurable(
        displayName = "Move Language",
        helpTopic = "Move_language_settings",
        _id = "org.move.settings"
    ) {

    private val chooseAptosCliPanel = ChooseAptosCliPanel(versionUpdateListener = null)
    private val chooseSuiCliPanel = ChooseSuiCliPanel()

    override fun createPanel(): DialogPanel {
        val configurablePanel =
            panel {
                val settings = project.moveSettings
                val state = settings.state.copy()

                group {
                    var aptosRadioButton: Cell<JBRadioButton>? = null
                    var suiRadioButton: Cell<JBRadioButton>? = null
                    buttonsGroup("Blockchain") {
                        row {
                            aptosRadioButton = radioButton("Aptos")
                                .bindSelected(
                                    { state.blockchain == Blockchain.APTOS },
                                    { state.blockchain = Blockchain.APTOS },
                                )
                            suiRadioButton = radioButton("Sui")
                                .bindSelected(
                                    { state.blockchain == Blockchain.SUI },
                                    { state.blockchain = Blockchain.SUI },
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
                        checkBox("Skip updating to the latest git dependencies")
                            .bindSelected(state::skipFetchLatestGitDeps)
                        comment(
                            "Adds --skip-fetch-latest-git-deps to the sync and test runs. " +
                                    "Speeds up projects refresh considerably."
                        )
                    }
                    row {
                        checkBox("Dump storage to console on test failures")
                            .bindSelected(state::dumpStateOnTestFailure)
                        comment(
                            "Adds --dump to the test runs (aptos only)."
                        )
                    }
                }

                if (!project.isDefault) {
                    row {
                        link("Set default project settings") {
                            ProjectManager.getInstance().defaultProject.showSettingsDialog<PerProjectMoveConfigurable>()
                        }
                            //                        .visible(true)
                            .align(AlignX.RIGHT)
                    }
                }

                // saves values from Swing form back to configurable (OK / Apply)
                onApply {
                    settings.modify {
                        it.aptosExecType = chooseAptosCliPanel.data.aptosExecType
                        it.localAptosPath = chooseAptosCliPanel.data.localAptosPath

                        it.localSuiPath = chooseSuiCliPanel.data.localSuiPath

                        it.blockchain = state.blockchain
                        it.foldSpecs = state.foldSpecs
                        it.disableTelemetry = state.disableTelemetry
                        it.debugMode = state.debugMode
                        it.skipFetchLatestGitDeps = state.skipFetchLatestGitDeps
                        it.dumpStateOnTestFailure = state.dumpStateOnTestFailure
                    }
                }

                // loads settings from configurable to swing form
                onReset {
                    chooseAptosCliPanel.data =
                        ChooseAptosCliPanel.Data(state.aptosExecType, state.localAptosPath)
                    chooseSuiCliPanel.data =
                        ChooseSuiCliPanel.Data(state.localSuiPath)
                }

                /// checks whether any settings are modified (should be fast)
                onIsModified {
                    val aptosPanelData = chooseAptosCliPanel.data
                    val suiPanelData = chooseSuiCliPanel.data
                    aptosPanelData.aptosExecType != settings.aptosExecType
                            || aptosPanelData.localAptosPath != settings.localAptosPath
                            || suiPanelData.localSuiPath != settings.localSuiPath
                }
            }
        Disposer.register(this.disposable!!, chooseAptosCliPanel)
        Disposer.register(this.disposable!!, chooseSuiCliPanel)
        return configurablePanel
    }

//    override fun disposeUIResources() {
//        super.disposeUIResources()
//        Disposer.dispose(chooseAptosCliPanel)
//        Disposer.dispose(chooseSuiCliPanel)
//    }
}
