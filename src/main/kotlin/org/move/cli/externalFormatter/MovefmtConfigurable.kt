package org.move.cli.externalFormatter

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import org.move.openapiext.pathField

class MovefmtConfigurable(val project: Project): BoundConfigurable("Movefmt") {
    private val innerDisposable = Disposer.newCheckedDisposable()

    private val movefmtPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            innerDisposable,
            "Movefmt location")
    private val additionalArguments: RawCommandLineEditor = RawCommandLineEditor()
    private val environmentVariables: EnvironmentVariablesComponent = EnvironmentVariablesComponent()


    override fun createPanel(): DialogPanel {
        this.disposable?.let {
            Disposer.register(it, innerDisposable)
        }
        return panel {
            val settings = project.movefmtSettings
            val state = settings.state.copy()

            row("Movefmt:") {
                cell(movefmtPathField)
                    .align(AlignX.FILL).resizableColumn()
                    .bind(
                        componentGet = { it.text },
                        componentSet = { component, value -> component.text = value },
                        prop = state::movefmtPath.toMutableProperty()
                    )
            }
            row("Additional arguments:") {
                cell(additionalArguments)
                    .align(AlignX.FILL)
                    .comment("Additional arguments to pass to <b>movefmt</b> command")
                    .bind(
                        componentGet = { it.text },
                        componentSet = { component, value -> component.text = value },
                        prop = state::additionalArguments.toMutableProperty()
                    )
            }
            row(environmentVariables.label) {
                cell(environmentVariables).align(AlignX.FILL)
                    .bind(
                        componentGet = { it.envs },
                        componentSet = { component, value -> component.envs = value },
                        prop = state::envs.toMutableProperty()
                    )
            }

            row { checkBox("Use movefmt instead of the built-in formatter").bindSelected(state::useMovefmt) }
//            row { checkBox("Run movefmt on Save").bindSelected(state::runRustfmtOnSave) }

            onApply {
                settings.modify {
                    it.movefmtPath = state.movefmtPath
                    it.additionalArguments = state.additionalArguments
                    it.envs = state.envs
                    it.useMovefmt = state.useMovefmt
//                    it.runRustfmtOnSave = state.runRustfmtOnSave
                }
            }
        }
    }
}