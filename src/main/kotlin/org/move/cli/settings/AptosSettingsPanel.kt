package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import org.move.cli.runConfigurations.aptos.AptosCliExecutor
import org.move.openapiext.UiDebouncer
import org.move.openapiext.pathField
import org.move.openapiext.showSettings
import org.move.stdext.toPathOrNull

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
            onAptosExecUpdate()
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
            onAptosExecUpdate()
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
                            onAptosExecUpdate()
                        }
                    )
            }

            row {
                val button = radioButton("Local")
                    .bindSelected(
                        { aptosExec is AptosExec.LocalPath },
                        {
                            aptosExec = AptosExec.LocalPath(localPathField.text)
                            onAptosExecUpdate()
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

    private fun onAptosExecUpdate() {
        val aptosExecPath = aptosExec.execPath.toPathOrNull()
        versionUpdateDebouncer.run(
            onPooledThread = {
                aptosExecPath?.let { AptosCliExecutor(it).version() }
            },
            onUiThread = { version ->
                versionLabel.setVersion(version)
                updateListener?.invoke()
            }
        )
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }
}
