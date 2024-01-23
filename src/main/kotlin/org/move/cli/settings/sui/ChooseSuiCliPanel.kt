package org.move.cli.settings.sui

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import org.move.cli.settings.MoveProjectSettingsService
import org.move.cli.settings.PerProjectMoveConfigurable
import org.move.cli.settings.VersionLabel
import org.move.openapiext.*
import org.move.stdext.toPathOrNull

class ChooseSuiCliPanel(
    private val versionUpdateListener: (() -> Unit)? = null
): Disposable {

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Sui CLI",
            onTextChanged = { text ->
                _suiCliPath = text
                _suiCliPath.toPathOrNull()?.let { versionLabel.updateValue(it) }
            })
    private val versionLabel = VersionLabel(this, versionUpdateListener)

    private lateinit var _suiCliPath: String

    fun getSuiCliPath(): String { return _suiCliPath }
    fun setSuiCliPath(path: String) {
        this._suiCliPath = path
        localPathField.text = path
        path.toPathOrNull()?.let { versionLabel.updateValue(it) }
    }

    fun attachToLayout(layout: Panel): Row {
        val panel = this
        if (!panel::_suiCliPath.isInitialized) {
            val defaultProjectSettings =
                ProjectManager.getInstance().defaultProject.getService(MoveProjectSettingsService::class.java)
            panel._suiCliPath = defaultProjectSettings.state.suiPath
        }
        val resultRow = with(layout) {
            group("Sui CLI") {
                row {
                    cell(localPathField)
                        .bindText(
                            { _suiCliPath },
                            { _suiCliPath = it }
                        )
                        .onChanged {
                            localPathField.text.toPathOrNull()?.let { versionLabel.updateValue(it) }
                        }
                        .align(AlignX.FILL).resizableColumn()
                }
                row("--version :") { cell(versionLabel) }
            }
        }
        _suiCliPath.toPathOrNull()?.let { versionLabel.updateValue(it) }
        return resultRow
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }
}
