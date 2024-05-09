/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.cli.externalLinter

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.*
import org.move.cli.util.RsCommandLineEditor
import org.move.openapiext.fullWidthCell

class MvExternalLinterConfigurable(val project: Project): BoundConfigurable("External Linters") {
    private val additionalArguments: RsCommandLineEditor =
        RsCommandLineEditor(project, RsCommandLineEditor.EmptyTextFieldCompletionProvider())
//        RsCommandLineEditor(project, CargoCommandCompletionProvider(project.cargoProjects, "check ") { null })

//    private val channelLabel: JLabel = Label(RsBundle.message("settings.rust.external.linters.channel.label"))
//    private val channel: ComboBox<RustChannel> = ComboBox<RustChannel>().apply {
//        RustChannel.values()
//            .sortedBy { it.index }
//            .forEach { addItem(it) }
//    }

    private val environmentVariables: EnvironmentVariablesComponent = EnvironmentVariablesComponent()

    override fun createPanel(): DialogPanel = panel {
        val settings = project.externalLinterSettings
        val state = settings.state.copy()

        row("External tool:") {
            comboBox(EnumComboBoxModel(ExternalLinter::class.java))
                .comment("External tool for additional code analysis")
                .bindItem(state::tool.toNullableProperty())
        }

        row("Additional arguments:") {
            fullWidthCell(additionalArguments)
                .resizableColumn()
                .comment("Additional arguments to pass to <b>aptos move compile</b> command")
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::additionalArguments.toMutableProperty()
                )

//            channelLabel.labelFor = channel
//            cell(channelLabel)
//            cell(channel)
//                .bind(
//                    componentGet = { it.item },
//                    componentSet = { component, value -> component.item = value },
//                    prop = state::channel.toMutableProperty()
//                )
        }

        row(environmentVariables.label) {
            fullWidthCell(environmentVariables)
                .bind(
                    componentGet = { it.envs },
                    componentSet = { component, value -> component.envs = value },
                    prop = state::envs.toMutableProperty()
                )
        }

        row {
            checkBox("Run external linter to analyze code on the fly")
                .comment("Adds code highlighting based on the external linter results. May affect the IDE performance")
                .bindSelected(state::runOnTheFly)
        }

        onApply {
            settings.modify {
                it.tool = state.tool
                it.additionalArguments = state.additionalArguments
//                it.channel = state.channel
                it.envs = state.envs
                it.runOnTheFly = state.runOnTheFly
            }
        }
    }
}
