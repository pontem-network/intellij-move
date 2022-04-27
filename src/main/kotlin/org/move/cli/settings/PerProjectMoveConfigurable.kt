package org.move.cli.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel

class PerProjectMoveConfigurable(val project: Project) : BoundConfigurable("Move Language"),
                                                         Configurable.NoScroll {

    private val binaryPathField = FilePathWithVersionField(project)

    private val state: MvProjectSettingsService.State = project.moveSettings.settingsState

    override fun createPanel(): DialogPanel {
        return panel {
            row("Move binary:") {
                binaryPathField.field()
            }
            row("Version") {
                binaryPathField.versionLabel()
            }
            buttonGroup {
                row {
                    radioButton("Aptos",
                                { state.projectType == ProjectType.APTOS },
                                { if (it) state.projectType = ProjectType.APTOS })
                    radioButton("Dove",
                                { state.projectType == ProjectType.DOVE },
                                { if (it) state.projectType = ProjectType.DOVE })
                }
            }
            row("Aptos private key") { textField(state::privateKey) }
            row { checkBox("Collapse specs by default", state::collapseSpecs) }
        }
    }
//    override fun createPanel(): DialogPanel {
//        return panel {
//            row("Move binary:") {
//                cell(binaryPathField.field)
//                    .horizontalAlign(HorizontalAlign.FILL)
//            }
//            row("Version") {
//                cell(binaryPathField.versionLabel).horizontalAlign(HorizontalAlign.LEFT)
//            }
//            buttonsGroup("", { state::projectType }) {
//                row {
//                    radioButton("Aptos", ProjectType.APTOS)
//                    radioButton("Dove", ProjectType.DOVE)
//                }
//            }.bind(state::projectType)
//            row("Aptos private key") {
//                textField()
//                    .bindText(state::privateKey)
//                    .horizontalAlign(HorizontalAlign.FILL)
//            }
//            row { checkBox("Collapse specs by default").bindSelected(state::collapseSpecs) }
//        }
//    }

    override fun disposeUIResources() {
        val kind = this.state.projectType
        val moveExecutablePath = binaryPathField.selectedMoveBinaryPath()
        val privateKey = this.state.privateKey
        val collapseSpecs = this.state.collapseSpecs
        project.moveSettings.settingsState = MvProjectSettingsService.State(
            kind,
            moveExecutablePath,
            privateKey,
            collapseSpecs
        )
        super.disposeUIResources()
        Disposer.dispose(binaryPathField)
    }
}