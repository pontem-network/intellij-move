package org.move.cli.transactions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.name
import org.move.lang.core.types.address
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.ty.TyAddress
import org.move.lang.core.types.ty.TyBool
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyVector
import org.move.utils.ui.*
import javax.swing.JComponent

const val NAME_COLUMNS = 42

//const val ARGUMENT_COLUMNS = 36
const val PROFILE_COLUMNS = 24

class RunTransactionDialog(
    val entryFunction: MvFunction,
    val moveProject: MoveProject,
    val profiles: List<String>,
) : DialogWrapper(entryFunction.project) {

    var configurationName: String

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
            val parameterBindings = entryFunction.allParamsAsBindings.drop(1)
            val itemContext =
                entryFunction.module?.itemContext(false) ?: project.itemContext(false)

            if (typeParameters.isNotEmpty() || parameterBindings.isNotEmpty()) {
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
            if (parameterBindings.isNotEmpty()) {
                val inferenceCtx = InferenceContext(false, itemContext)
                group("Value Arguments") {
                    for (parameter in parameterBindings) {
                        val paramName = parameter.name
                        val paramTy = inferenceCtx.getBindingPatTy(parameter)
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

    fun toAptosCommandLine(): AptosCommandLine? {
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

        val functionParamNames = entryFunction.parameters.map { it.name }
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
        return AptosCommandLine(
            "move run",
            workingDirectory = moveProject.contentRootPath,
            arguments = commandArgs
        )
    }

    companion object {
        fun showAndWaitTillOk(
            entryFunction: MvFunction,
            moveProject: MoveProject
        ): RunTransactionDialog? {
            val project = moveProject.project
            // TODO: show notification that user needs to run `aptos init` first for transaction dialog to work
            val aptosConfig = moveProject.currentPackage.aptosConfigYaml
            if (aptosConfig == null) {
                // ask user to run `aptos init` first
                val notif = NotificationGroupManager.getInstance()
                    .getNotificationGroup("Missing Aptos Init")
                    .createNotification(
                        "Aptos account is not initialized, run `aptos init` first",
                        NotificationType.WARNING
                    )
                Notifications.Bus.notify(notif, project)
                return null
            }

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
