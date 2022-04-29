package org.move.cli.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.not
import com.intellij.ui.layout.panel

class PerProjectMoveConfigurable(val project: Project) : BoundConfigurable("Move Language"),
                                                         Configurable.NoScroll {

    private val state: MvProjectSettingsService.State = project.moveSettings.settingsState

    private val binaryPathField = FilePathWithVersionField(project)

    override fun createPanel(): DialogPanel {
        return panel {
            titledRow("") {
                row("Aptos CLI") {
                    binaryPathField.field()
                    comment("Required").visibleIf(binaryPathField.valid.not())
                }
                row("Version") {
                    binaryPathField.versionLabel()
                }
            }
            row("Aptos private key") { textField(state::privateKey) }
            titledRow("") {
                row { checkBox("Collapse specs by default", state::collapseSpecs) }
            }
        }
    }

    override fun disposeUIResources() {
        val moveExecutablePath = binaryPathField.selectedMoveBinaryPath()
        val privateKey = this.state.privateKey
        val collapseSpecs = this.state.collapseSpecs
        project.moveSettings.settingsState = MvProjectSettingsService.State(
            moveExecutablePath,
            privateKey,
            collapseSpecs
        )
        super.disposeUIResources()
        Disposer.dispose(binaryPathField)
    }
}
