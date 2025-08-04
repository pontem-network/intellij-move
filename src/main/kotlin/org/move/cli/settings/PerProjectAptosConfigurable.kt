package org.move.cli.settings

import com.intellij.openapi.externalSystem.service.settings.ExternalSystemGroupConfigurable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import org.move.cli.settings.aptos.ChooseAptosCliPanel
import org.move.openapiext.fullWidthCell
import org.move.openapiext.showSettingsDialog

// panels needs not to be bound to the Configurable itself, as it's sometimes created without calling the `createPanel()`
class PerProjectAptosConfigurable(val project: Project): BoundConfigurable("Aptos") {

    private val extraTextArgsField = ExpandableTextField()

    override fun createPanel(): DialogPanel {
        val chooseAptosCliPanel = ChooseAptosCliPanel(versionUpdateListener = null)
        this.disposable?.let {
            Disposer.register(it, chooseAptosCliPanel)
        }
        return panel {
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
                row {
                    checkBox("Enable Move V2")
                        .comment(
                            "Enables Move 2 language features"
                        )
                        .bindSelected(state::enableMove2)
                }
                group("Extra CLI arguments") {
                    row {
                        fullWidthCell(extraTextArgsField)
                            .resizableColumn()
                            .comment(
                                "Additional arguments to pass to <b>aptos move test</b>"
                            )
                            .bind(
                                componentGet = { it.text },
                                componentSet = { component, value -> component.text = value },
                                prop = state::extraTestArgs.toMutableProperty()
                            )
                    }
                    row {
                        checkBox("Disable telemetry for new Run Configurations")
                            .comment(
                                "Adds APTOS_DISABLE_TELEMETRY=true to every generated Aptos command."
                            )
                            .bindSelected(state::disableTelemetry)
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
                    val localAptosSdkPath = chooseAptosCliPanel.data.aptosPath
                    if (localAptosSdkPath != null) {
                        chooseAptosCliPanel.updateAptosSdks(localAptosSdkPath)
                    }
                    it.aptosPath = localAptosSdkPath

                    it.disableTelemetry = state.disableTelemetry
                    it.extraTestArgs = state.extraTestArgs
                    it.enableMove2 = state.enableMove2
                    it.fetchAptosDeps = state.fetchAptosDeps
                }
            }

            // loads settings from configurable to swing form
            onReset {
                chooseAptosCliPanel.data = ChooseAptosCliPanel.Data(state.aptosPath)
            }

            /// checks whether any settings are modified (should be fast)
            onIsModified {
                val aptosPanelData = chooseAptosCliPanel.data
                aptosPanelData.aptosPath != settings.aptosPath
            }
        }
    }
}
