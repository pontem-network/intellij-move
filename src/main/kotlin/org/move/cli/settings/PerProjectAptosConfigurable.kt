package org.move.cli.settings

import com.intellij.openapi.externalSystem.service.settings.ExternalSystemGroupConfigurable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
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
                        checkBox("Fetch external packages on project reload")
                            .bindSelected(state::fetchAptosDeps)
                        link("Configure project reload schedule") {
                            ProjectManager.getInstance().defaultProject.showSettingsDialog<ExternalSystemGroupConfigurable>()
                        }
                            .align(AlignX.RIGHT)
                    }
                    group("Compiler V2") {
                        row {
                            checkBox("Set Compiler V2 for CLI")
                                .comment(
                                    "Adds `--compiler-version v2 --language-version 2.0` " +
                                            "to all generated Aptos CLI commands"
                                )
                                .bindSelected(state::addCompilerV2CLIFlags)
                        }
                        row {
                            checkBox("Enable resource-access control")
                                .comment(
                                    "Enables resource access control specifies " +
                                            "(<code>reads, writes, pure</code> for functions) in the parser. " +
                                            "Requires re-parsing of all Move files in the project, can be slow."
                                )
                                .bindSelected(state::enableResourceAccessControl)
                        }
                        row {
                            checkBox("Enable indexing")
                                .comment(
                                    "Enables resource and vector indexing (i.e. v[0], R[@0x1]) " +
                                            "for the Move files."
                                )
                                .bindSelected(state::enableIndexExpr)
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
                        it.enableResourceAccessControl = state.enableResourceAccessControl
                        it.enableIndexExpr = state.enableIndexExpr
                        it.addCompilerV2CLIFlags = state.addCompilerV2CLIFlags
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
