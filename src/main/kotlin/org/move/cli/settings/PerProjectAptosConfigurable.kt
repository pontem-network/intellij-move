package org.move.cli.settings

import com.intellij.openapi.externalSystem.service.settings.ExternalSystemGroupConfigurable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import org.move.cli.settings.aptos.ChooseAptosCliPanel
import org.move.openapiext.showSettingsDialog

// panels needs not to be bound to the Configurable itself, as it's sometimes created without calling the `createPanel()`
class PerProjectAptosConfigurable(val project: Project): BoundConfigurable("Aptos") {

    override fun createPanel(): DialogPanel {
        val chooseAptosCliPanel = ChooseAptosCliPanel(versionUpdateListener = null)
        val configurablePanel =
            panel {
                val settings = project.moveSettings
                val state = settings.state.copy()

                chooseAptosCliPanel.attachToLayout(this)

                group {
                    row {
                        checkBox("Fetch external dependencies on project reload")
                            .bindSelected(state::fetchAptosDeps)
                        link("Configure project reload schedule") {
                            ProjectManager.getInstance().defaultProject.showSettingsDialog<ExternalSystemGroupConfigurable>()
                        }
                            .align(AlignX.RIGHT)
                    }
                    val compilerV2Box = JBCheckBox("Enable V2 compiler")
                    row {
                        cell(compilerV2Box)
                            .comment(
                                "Enables features of the Aptos V2 compiler " +
                                        "(receiver style functions, access control, etc.)"
                            )
                            .bindSelected(state::isCompilerV2)
                    }
                    indent {
                        row {
                            checkBox("Set Compiler V2 in CLI commands")
                                .comment("Adds `--compiler-version v2 --language-version 2.0` to all generated Aptos CLI commands")
                                .enabledIf(compilerV2Box.selected)
                                .bindSelected(state::addCompilerV2Flags)
                        }
                    }
                    group("Command Line Options") {
                        row {
                            checkBox("Disable telemetry for new Run Configurations")
                                .comment(
                                    "Adds APTOS_DISABLE_TELEMETRY=true to every generated Aptos command."
                                )
                                .bindSelected(state::disableTelemetry)
                        }
                        row {
                            checkBox("Skip updating to the latest git dependencies")
                                .comment(
                                    "Adds --skip-fetch-latest-git-deps to the sync and test runs."
                                )
                                .bindSelected(state::skipFetchLatestGitDeps)

                        }
                        row {
                            checkBox("Dump storage to console on test failures")
                                .comment(
                                    "Adds --dump to generated test runs."
                                )
                                .bindSelected(state::dumpStateOnTestFailure)
                        }
                    }
                }

                if (!project.isDefault) {
                    row {
                        link("Set default project settings") {
                            ProjectManager.getInstance().defaultProject.showSettingsDialog<PerProjectAptosConfigurable>()
                        }
                            .align(AlignX.RIGHT)
                    }
                }

                // saves values from Swing form back to configurable (OK / Apply)
                onApply {
                    settings.modify {
                        it.aptosExecType = chooseAptosCliPanel.data.aptosExecType

                        val localAptosSdkPath = chooseAptosCliPanel.data.localAptosPath
                        if (localAptosSdkPath != null) {
                            chooseAptosCliPanel.updateAptosSdks(localAptosSdkPath)
                        }
                        it.localAptosPath = localAptosSdkPath

                        it.disableTelemetry = state.disableTelemetry
                        it.skipFetchLatestGitDeps = state.skipFetchLatestGitDeps
                        it.dumpStateOnTestFailure = state.dumpStateOnTestFailure
                        it.isCompilerV2 = state.isCompilerV2
                        it.addCompilerV2Flags = state.addCompilerV2Flags
                        it.fetchAptosDeps = state.fetchAptosDeps
                    }
                }

                // loads settings from configurable to swing form
                onReset {
                    chooseAptosCliPanel.data =
                        ChooseAptosCliPanel.Data(state.aptosExecType, state.localAptosPath)
                }

                /// checks whether any settings are modified (should be fast)
                onIsModified {
                    val aptosPanelData = chooseAptosCliPanel.data
                    aptosPanelData.aptosExecType != settings.aptosExecType
                            || aptosPanelData.localAptosPath != settings.localAptosPath
                }
            }
        this.disposable?.let {
            Disposer.register(it, chooseAptosCliPanel)
        }
        return configurablePanel
    }

//    override fun disposeUIResources() {
//        super.disposeUIResources()
//        Disposer.dispose(chooseAptosCliPanel)
//    }
}