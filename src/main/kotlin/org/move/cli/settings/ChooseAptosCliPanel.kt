package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import org.move.cli.runConfigurations.aptos.AptosCliExecutor
import org.move.openapiext.UiDebouncer
import org.move.openapiext.isSuccess
import org.move.openapiext.pathField
import org.move.openapiext.showSettings

class ChooseAptosCliPanel(
    private val showDefaultProjectSettingsLink: Boolean,
    private val updateListener: (() -> Unit)? = null
): Disposable {

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Aptos CLI",
            onTextChanged = { text ->
                val exec = AptosExec.LocalPath(text)
                _aptosExec = exec
                updateVersionLabel(exec)
            })

    private val versionLabel = VersionLabel()
    private val versionUpdateDebouncer = UiDebouncer(this)

    private lateinit var _aptosExec: AptosExec

    var selectedAptosExec: AptosExec
        get() = _aptosExec
        set(aptosExec) {
            this._aptosExec = aptosExec
            when (_aptosExec) {
                is AptosExec.Bundled -> localPathField.text = ""
                else ->
                    localPathField.text = aptosExec.execPath
            }
            updateVersionLabel(aptosExec)
        }

    fun attachToLayout(layout: Panel): Row {
        val panel = this
        if (!panel::_aptosExec.isInitialized) {
            panel._aptosExec = AptosExec.default()
        }
        val resultRow = with(layout) {
            buttonsGroup("Aptos CLI") {
                row {
                    radioButton("Bundled")
                        .bindSelected(
                            { _aptosExec is AptosExec.Bundled },
                            {
                                _aptosExec = AptosExec.Bundled
                            }
                        )
                        .onChanged {
                            updateVersionLabel(AptosExec.Bundled)
                        }
                        .enabled(AptosExec.isBundledSupportedForThePlatform())
                    comment(
                        "Bundled version is not available for this platform (refer to the official Aptos docs for more)"
                    )
                        .visible(!AptosExec.isBundledSupportedForThePlatform())
                }

                row {
                    val button = radioButton("Local")
                        .bindSelected(
                            { _aptosExec is AptosExec.LocalPath },
                            {
                                _aptosExec = AptosExec.LocalPath("")
//                                updateVersionLabel()
                            }
                        )
                        .onChanged {
                            updateVersionLabel(AptosExec.LocalPath(localPathField.text))
                        }

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
        updateVersionLabel(_aptosExec)
        return resultRow
    }

    private fun updateVersionLabel(withExec: AptosExec) {
        val aptosExecPath = withExec.toPathOrNull()
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
