package org.move.cli.transactions

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.LanguageTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.PsiErrorElementUtil
import org.move.cli.AptosCommandLine
import org.move.cli.MoveProject
import org.move.cli.runconfig.producers.AptosCommandLineFromContext
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.inferBindingTy
import org.move.lang.core.psi.module
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.address
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.ty.TyAddress
import org.move.lang.core.types.ty.TyBool
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyVector
import org.move.utils.ui.MoveTextFieldWithCompletion
import org.move.utils.ui.bindText
import org.move.utils.ui.registerValidationRequestor
import org.move.utils.ui.ulongTextField
import javax.swing.JComponent

const val NAME_COLUMNS = 42
const val ARGUMENT_COLUMNS = 36
const val PROFILE_COLUMNS = 24

class RunTransactionDialog(
    val entryFunction: MvFunction,
    val moveProject: MoveProject,
    val profiles: List<String>,
) : DialogWrapper(entryFunction.project) {

    private var configurationName: String
    private var selectedProfile: String?
    private val typeParams = mutableMapOf<String, String>()
    private val params = mutableMapOf<String, String>()

    init {
        title = "Transaction Parameters"
        configurationName = "Run ${entryFunction.fqName}"
        selectedProfile = if (profiles.contains("default")) "default" else profiles[0]
        init()
    }

    override fun createCenterPanel(): JComponent {
        val project = entryFunction.project
        val cacheService = project.service<RunTransactionCacheService>()
        return panel {
            row("Run Configuration name: ") {
                textField()
                    .bindText({ configurationName }, { configurationName = it })
                    .columns(NAME_COLUMNS)
                    .horizontalAlign(HorizontalAlign.RIGHT)
            }
            val typeParameters = entryFunction.typeParameters
            val parameters = entryFunction.parameterBindings().drop(1)
            val itemContext = entryFunction.module?.itemContext(false) ?: project.itemContext(false)

            if (typeParameters.isNotEmpty() || parameters.isNotEmpty()) {
                separator()
            }

            if (typeParameters.isNotEmpty()) {
                group("Type Arguments") {
                    for (typeParameter in entryFunction.typeParameters) {
                        val paramName = typeParameter.name ?: continue
                        row(paramName) {
                            val previousValues = cacheService.getTypeParameterCache(paramName)
                            cell(typeParameterTextField(previousValues))
//                                .columns(ARGUMENT_COLUMNS)
                                .horizontalAlign(HorizontalAlign.FILL)
                                .bindText(
                                    { typeParams.getOrDefault(paramName, "") },
                                    { typeParams[paramName] = it })
                                .registerValidationRequestor()
                                .validationOnApply(validateEditorTextNonEmpty("Required type parameter"))
                                .validationOnApply(validateParseErrors("Invalid type format"))
                        }
                    }
                }
            }
            if (parameters.isNotEmpty()) {
                group("Value Arguments") {
                    for (parameter in parameters) {
                        val paramName = parameter.name ?: continue
                        val paramTy = parameter.inferBindingTy(InferenceContext(false), itemContext)
                        val paramTyName = when (paramTy) {
                            is TyInteger -> paramTy.kind.name
                            is TyAddress -> "address"
                            is TyBool -> "bool"
                            is TyVector -> "vector"
                            else -> "unknown"
                        }
                        row(paramName) {
                            comment(": $paramTyName")
                            when (paramTy) {
                                is TyInteger -> ulongTextField(paramTy.ulongRange())
                                else -> textField()
                            }
                                .bindText(
                                    { params.getOrDefault(paramName, "") },
                                    {
                                        if (it.isNotBlank()) {
                                            params[paramName] = "$paramTyName:$it"
                                        }
                                    })
//                                .columns(ARGUMENT_COLUMNS)
                                .horizontalAlign(HorizontalAlign.FILL)
                                .validationOnApply(validateNonEmpty("Required parameter"))

                        }
                    }
                }
            }
            separator()
            if (profiles.isNotEmpty()) {
                row("Profile:") {
                    comboBox(profiles)
                        .enabled(profiles.size > 1)
                        .bindItem({ selectedProfile ?: profiles[0] }, { selectedProfile = it })
                        .columns(PROFILE_COLUMNS)
                        .horizontalAlign(HorizontalAlign.RIGHT)
                }
            }
        }
    }

    private fun typeParameterTextField(variants: Collection<String>): LanguageTextField {
        val project = entryFunction.project
        // TODO: add TYPE icon
        val completionProvider = TextFieldWithAutoCompletion.StringsCompletionProvider(variants, null)
        return MoveTextFieldWithCompletion(
            project,
            "",
            completionProvider,
            entryFunction,
        )
    }

    fun toAptosCommandLineFromContext(): AptosCommandLineFromContext? {
        val address = entryFunction.module?.address(moveProject)?.canonicalValue ?: return null
        val module = entryFunction.module?.name ?: return null
        val name = entryFunction.name ?: return null

        val functionTypeParamNames = entryFunction.typeParameters.mapNotNull { it.name }
        val sortedTypeParams = this.typeParams
            .entries
            .sortedBy { (name, _) ->
                functionTypeParamNames.indexOfFirst { it == name }
            }.flatMap { (_, value) ->
                listOf(
                    "--type-args",
                    maybeQuoteTypeArg(value)
                )
            }

        val functionParamNames = entryFunction.parameterBindings().mapNotNull { it.name }
        val sortedParams = this.params.entries
            .sortedBy { (name, _) ->
                functionParamNames.indexOfFirst { it == name }
            }.flatMap { (_, value) -> listOf("--args", value) }

        val profile = this.selectedProfile
        val profileArgs =
            if (profile != null) listOf("--profile", profile) else listOf()
        val commandArgs = listOf(
            profileArgs,
            listOf("--function-id", "${address}::${module}::${name}"),
            sortedTypeParams,
            sortedParams,
        ).flatten()
        val commandLine =
            AptosCommandLine("move run", moveProject.contentRootPath, commandArgs)
        return AptosCommandLineFromContext(
            entryFunction, this.configurationName, commandLine
        )
    }

    companion object {
        fun showAndGetOk(
            entryFunction: MvFunction,
            moveProject: MoveProject
        ): RunTransactionDialog? {
            // TODO: show dialog that user needs to run `aptos init` first for transaction dialog to work
            val aptosConfig = moveProject.currentPackage.aptosConfigYaml ?: return null

            val profiles = aptosConfig.profiles.toList()
            val dialog = RunTransactionDialog(entryFunction, moveProject, profiles)
            val isOk = dialog.showAndGet()
            if (!isOk) return null

            val cacheService = moveProject.project.cacheService
            for ((name, value) in dialog.typeParams.entries) {
                cacheService.cacheTypeParameter(name, value)
            }
            return dialog
        }

        private fun maybeQuoteTypeArg(typeArg: String): String =
            if (typeArg.contains('<') || typeArg.contains('>')) {
                "\"$typeArg\""
            } else {
                typeArg
            }

    }
}

private fun validateNonEmpty(message: String): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
    return {
        if (it.text.isNullOrEmpty()) error(message) else null
    }
}

private fun validateEditorTextNonEmpty(message: String): ValidationInfoBuilder.(LanguageTextField) -> ValidationInfo? {
    return {
        if (it.text.isEmpty()) error(message) else null

    }
}

private fun validateParseErrors(message: String): ValidationInfoBuilder.(LanguageTextField) -> ValidationInfo? {
    return {
        FileDocumentManager.getInstance().getFile(it.document)
            ?.let { file ->
                if (PsiErrorElementUtil.hasErrors(it.project, file)) {
                    error(message)
                } else null
            }
    }
}
