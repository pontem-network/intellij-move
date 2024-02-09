package org.move.cli.settings.aptos

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import org.move.cli.settings.VersionLabel
import org.move.openapiext.pathField
import org.move.stdext.toPathOrNull

class ChooseAptosCliPanel(
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
                exec.toPathOrNull()?.let { versionLabel.updateValueWithListener(it) }
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
            aptosExec.toPathOrNull()?.let { versionLabel.updateValueWithListener(it) }
        }

    fun attachToLayout(layout: Panel): Row {
        val panel = this
        if (!panel::_aptosExec.isInitialized) {
            panel._aptosExec = AptosExec.default()
        }
        val resultRow = with(layout) {
            group("Aptos CLI") {
                buttonsGroup {
                    row {
                        radioButton("Bundled", AptosExec.Bundled)
                            .actionListener { _, _ ->
                                _aptosExec = AptosExec.Bundled
                                AptosExec.Bundled.toPathOrNull()
                                    ?.let { versionLabel.updateValueWithListener(it) }
                            }
                            .enabled(AptosExec.isBundledSupportedForThePlatform())
                        comment(
                            "Bundled version is not available for this platform (refer to the official Aptos docs for more)"
                        )
                            .visible(!AptosExec.isBundledSupportedForThePlatform())
                    }
                    row {
                        val button = radioButton("Local", AptosExec.LocalPath(""))
                            .actionListener { _, _ ->
                                _aptosExec = AptosExec.LocalPath(localPathField.text)
                                localPathField.text.toPathOrNull()
                                    ?.let { versionLabel.updateValueWithListener(it) }
                            }
                        cell(localPathField)
                            .enabledIf(button.selected)
                            .align(AlignX.FILL).resizableColumn()
                    }
                    row("--version :") { cell(versionLabel) }
                }
                    .bind(
                        { _aptosExec },
                        {
                            _aptosExec =
                                when (it) {
                                    is AptosExec.Bundled -> it
                                    is AptosExec.LocalPath -> AptosExec.LocalPath(localPathField.text)
                                }
                        }
                    )
            }
        }
        _aptosExec.toPathOrNull()?.let { versionLabel.updateValueWithListener(it) }
        return resultRow
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }
}
