package org.move.cli.settings.aptos

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selected
import org.move.cli.settings.VersionLabel
import org.move.cli.settings.aptos.AptosExecType.BUNDLED
import org.move.cli.settings.aptos.AptosExecType.LOCAL
import org.move.openapiext.PluginPathManager
import org.move.openapiext.pathField
import org.move.stdext.blankToNull
import org.move.stdext.toPathOrNull

enum class AptosExecType {
    BUNDLED,
    LOCAL;

    companion object {
        val isBundledSupportedForThePlatform: Boolean get() = !SystemInfo.isMac
//        val isBundledSupportedForThePlatform: Boolean get() = false

        fun bundledPath(): String? = PluginPathManager.bundledAptosCli
        fun aptosPath(execType: AptosExecType, localAptosPath: String?): String {
            return when (execType) {
                BUNDLED -> bundledPath() ?: ""
                LOCAL -> localAptosPath ?: ""
            }
        }
    }
}

class ChooseAptosCliPanel(versionUpdateListener: (() -> Unit)?): Disposable {

    data class Data(
        val aptosExecType: AptosExecType,
        val localAptosPath: String?
    )

    var data: Data
        get() {
            val execType = if (bundledRadioButton.isSelected) BUNDLED else LOCAL
            val path = localPathField.text.blankToNull()
            return Data(
                aptosExecType = execType,
                localAptosPath = path
            )
        }
        set(value) {
            when (value.aptosExecType) {
                BUNDLED -> {
                    bundledRadioButton.isSelected = true
                    localRadioButton.isSelected = false
                }
                LOCAL -> {
                    bundledRadioButton.isSelected = false
                    localRadioButton.isSelected = true
                }
            }
            localPathField.text = value.localAptosPath ?: ""
            updateVersion()
        }

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Aptos CLI",
            onTextChanged = { _ ->
                updateVersion()
            })
    private val versionLabel = VersionLabel(this, versionUpdateListener)

    private val bundledRadioButton = JBRadioButton("Bundled")
    private val localRadioButton = JBRadioButton("Local")

    fun attachToLayout(layout: Panel): Row {
        val resultRow = with(layout) {
            group("Aptos CLI") {
                buttonsGroup {
                    row {
                        cell(bundledRadioButton)
                            .enabled(AptosExecType.isBundledSupportedForThePlatform)
                            .actionListener { _, _ ->
                                updateVersion()
                            }
                        comment(
                            "Bundled version is not available for this platform (refer to the official Aptos docs for more)"
                        )
                            .visible(!AptosExecType.isBundledSupportedForThePlatform)
                    }
                    row {
                        cell(localRadioButton)
                            .actionListener { _, _ ->
                                updateVersion()
                            }
                        cell(localPathField )
                            .enabledIf(localRadioButton.selected)
                            .align(AlignX.FILL)
                            .resizableColumn()
                    }
                    row("--version :") { cell(versionLabel) }
                }
            }
        }
        updateVersion()
        return resultRow
    }

    private fun updateVersion() {
        val aptosPath =
            when {
                bundledRadioButton.isSelected -> AptosExecType.bundledPath()
                else -> localPathField.text
            }?.toPathOrNull()
        versionLabel.updateAndNotifyListeners(aptosPath)
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }
}
