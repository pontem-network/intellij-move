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
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.dsl.builder.*
import org.move.openapiext.fullWidthCell

class MvExternalLinterConfigurable(val project: Project): BoundConfigurable("External Linters") {

    private val additionalArguments = ExpandableTextField()
//    private val additionalArguments: RsCommandLineEditor =
//        RsCommandLineEditor(project, RsCommandLineEditor.EmptyTextFieldCompletionProvider())

    private val environmentVariables: EnvironmentVariablesComponent = EnvironmentVariablesComponent()

    override fun createPanel(): DialogPanel = panel {
        val settings = project.externalLinterSettings
        val state = settings.state.copy()

        row {
            checkBox("Run external linter on the fly")
                .comment("Adds code highlighting based on the external linter results. May affect the IDE performance")
                .bindSelected(state::runOnTheFly)
        }
        row("External tool:") {
            comboBox(EnumComboBoxModel(ExternalLinter::class.java))
                .comment("External tool for additional code analysis")
                .bindItem(state::tool.toNullableProperty())
        }

        row("Additional arguments:") {
            fullWidthCell(additionalArguments)
                .resizableColumn()
                .comment(
                    "Additional arguments to pass to <b>endless move compile</b> / <b>endless move lint</b>"
                )
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::additionalArguments.toMutableProperty()
                )
        }

        row(environmentVariables.label) {
            fullWidthCell(environmentVariables)
                .bind(
                    componentGet = { it.envs },
                    componentSet = { component, value -> component.envs = value },
                    prop = state::envs.toMutableProperty()
                )
        }

        separator()
        row {
            checkBox("Prevent duplicate errors")
                .comment("Skips errors which are implemented by the IDE's own analysis engine.")
                .bindSelected(state::skipErrorsKnownToIde)
        }

        onApply {
            settings.modify {
                it.tool = state.tool
                it.additionalArguments = state.additionalArguments
                it.envs = state.envs
                it.runOnTheFly = state.runOnTheFly
                it.skipErrorsKnownToIde = state.skipErrorsKnownToIde
            }
        }
    }
}
