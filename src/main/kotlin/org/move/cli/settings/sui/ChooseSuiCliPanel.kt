package org.move.cli.settings.sui

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import org.move.cli.settings.VersionLabel
import org.move.openapiext.pathField
import org.move.stdext.toPathOrNull

class ChooseSuiCliPanel(versionUpdateListener: (() -> Unit)? = null): Disposable {

    data class Data(
        val localSuiPath: String?,
    )

    var data: Data
        get() {
            return Data(localSuiPath = localPathField.text)
        }
        set(value) {
            localPathField.text = value.localSuiPath ?: ""
            updateVersion()
        }

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Sui CLI",
            onTextChanged = { _ ->
                updateVersion()
            })
    private val versionLabel = VersionLabel(this, versionUpdateListener)

    fun attachToLayout(layout: Panel): Row {
        val resultRow = with(layout) {
            group("Sui CLI") {
                row {
                    cell(localPathField)
                        .align(AlignX.FILL)
                        .resizableColumn()
                }
                row("--version :") { cell(versionLabel) }
            }
        }
        updateVersion()
        return resultRow
    }

    private fun updateVersion() {
        val localSuiPath = localPathField.text.toPathOrNull()
        versionLabel.updateAndNotifyListeners(localSuiPath)
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }
}
