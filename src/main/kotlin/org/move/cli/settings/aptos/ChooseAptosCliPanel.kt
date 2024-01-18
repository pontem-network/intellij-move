package org.move.cli.settings.aptos

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import org.move.cli.settings.PerProjectMoveConfigurable
import org.move.cli.settings.VersionLabel
import org.move.openapiext.*
import org.move.stdext.toPathOrNull

class ChooseAptosCliPanel(
    private val showDefaultProjectSettingsLink: Boolean,
    private val versionUpdateListener: (() -> Unit)? = null
): Disposable {

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Aptos CLI",
            onTextChanged = { text ->
                val exec = AptosExec.LocalPath(text)
                _aptosExec = exec
                exec.toPathOrNull()?.let { versionLabel.updateValue(it) }
            })

    private val versionLabel = VersionLabel(this, versionUpdateListener)

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
            aptosExec.toPathOrNull()?.let { versionLabel.updateValue(it) }
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
                            AptosExec.Bundled.toPathOrNull()?.let { versionLabel.updateValue(it) }
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
                            localPathField.text.toPathOrNull()?.let { versionLabel.updateValue(it) }
                        }

                    cell(localPathField)
                        .enabledIf(button.selected)
                        .align(AlignX.FILL).resizableColumn()
                }
            }
            row("--version :") { cell(versionLabel) }
            row {
                link("Set default project settings") {
                    ProjectManager.getInstance().defaultProject.showSettings<PerProjectMoveConfigurable>()
                }
                    .visible(showDefaultProjectSettingsLink)
                    .align(AlignX.RIGHT)
                //                .horizontalAlign(HorizontalAlign.RIGHT)
            }
        }
        _aptosExec.toPathOrNull()?.let { versionLabel.updateValue(it) }
        return resultRow
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }
}
