package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComponentPredicate
import org.move.cli.runConfigurations.aptos.AptosCliExecutor
import org.move.openapiext.UiDebouncer
import org.move.openapiext.isSuccess
import org.move.openapiext.pathField
import org.move.openapiext.showSettings
import org.move.stdext.toPathOrNull
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class AptosSettingsPanel(
    private val showDefaultProjectSettingsLink: Boolean,
    private val updateListener: (() -> Unit)? = null
): Disposable {

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Aptos CLI"
        ) { text ->
            aptosExec = AptosExec.LocalPath(text)
            updateVersionLabel()
        }

    private val versionLabel = VersionLabel()
    private val versionUpdateDebouncer = UiDebouncer(this)

    data class PanelData(val aptosExec: AptosExec)

    var panelData: PanelData
        get() = PanelData(aptosExec)
        set(value) {
            when (value.aptosExec) {
                is AptosExec.Bundled -> localPathField.text = ""
                else -> localPathField.text = value.aptosExec.execPath
            }
            updateVersionLabel()
        }

    var aptosExec: AptosExec = AptosExec.Bundled

    fun attachTo(layout: Panel) = with(layout) {
        // Don't use `Project.toolchain` or `Project.rustSettings` here because
        // `getService` can return `null` for default project after dynamic plugin loading.
        // As a result, you can get `java.lang.IllegalStateException`
        // So let's handle it manually
        val defaultProjectSettings =
            ProjectManager.getInstance().defaultProject.getService(MoveProjectSettingsService::class.java)
        panelData = PanelData(
            aptosExec = AptosExec.fromSettingsFormat(defaultProjectSettings.state.aptosPath),
        )
        buttonsGroup("Aptos CLI") {
            row {
                radioButton("Bundled")
                    .bindSelected(
                        { aptosExec is AptosExec.Bundled },
                        {
                            aptosExec = AptosExec.Bundled
                            updateVersionLabel()
                        }
                    )
                    .enabled(AptosExec.isBundledSupported())
                comment("Disabled on MacOS, use `brew install aptos` to obtain the aptos-cli")
                    .visible(!AptosExec.isBundledSupported())
            }

            row {
                val button = radioButton("Local")
                    .bindSelected(
                        { aptosExec is AptosExec.LocalPath },
                        {
                            aptosExec = AptosExec.LocalPath(localPathField.text)
                            updateVersionLabel()
                        }
                    )
                cell(localPathField)
                    .enabledIf(button.selected)
                    .align(AlignX.FILL).resizableColumn()
            }
        }
        row("aptos --version :") { cell(versionLabel) }
        row {
            link("Set default project settings") {
                ProjectManager.getInstance().defaultProject.showSettings<PerProjectMoveConfigurable>()
            }
                .visible(showDefaultProjectSettingsLink)
                .align(AlignX.RIGHT)
//                .horizontalAlign(HorizontalAlign.RIGHT)
        }
    }

    private fun updateVersionLabel() {
        val aptosExecPath = aptosExec.execPath.toPathOrNull()
        versionUpdateDebouncer.run(
            onPooledThread = {
                aptosExecPath?.let { AptosCliExecutor(it).version() }
            },
            onUiThread = { versionCmdOutput ->
                if (versionCmdOutput == null) {
                    versionLabel.setText("N/A (Invalid aptos executable)", errorHighlighting = true)
                } else {
                    if (versionCmdOutput.isSuccess) {
                        val versionText = versionCmdOutput.stdoutLines.joinToString("\n")
                        versionLabel.setText(versionText, errorHighlighting = false)
                    } else {
                        versionLabel.setText(
                            "N/A (Cannot run --version command. Error code is ${versionCmdOutput.exitCode})",
                            errorHighlighting = true
                        )
                    }
                }
                updateListener?.invoke()
            }
        )
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }
}
