package org.move.cli.transactions

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.LanguageTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.PsiErrorElementUtil
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.inferBindingTy
import org.move.lang.core.psi.module
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.ItemContext
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.ty.TyAddress
import org.move.lang.core.types.ty.TyBool
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyVector
import org.move.utils.ui.MoveTextFieldWithCompletion
import org.move.utils.ui.bindText
import org.move.utils.ui.registerValidationRequestor
import javax.swing.JComponent

const val NAME_COLUMNS = 42
const val ARGUMENT_COLUMNS = 36
const val PROFILE_COLUMNS = 24

fun Row.ulongTextField(range: ULongRange?): Cell<JBTextField> {
    val result = cell(JBTextField())
        .validationOnInput {
            val value = it.text.toULongOrNull()
            when {
                value == null -> error(UIBundle.message("please.enter.a.number"))
                range != null && value !in range -> error(
                    UIBundle.message(
                        "please.enter.a.number.from.0.to.1",
                        range.first,
                        range.last
                    )
                )
                else -> null
            }
        }
    result.component.putClientProperty("dsl.intText.range", range)
    return result
}

class TransactionParametersDialog(
    val scriptFunction: MvFunction,
    val profiles: List<String>,
) : DialogWrapper(scriptFunction.project) {

    var configurationName: String
    var selectedProfile: String?
    val typeParams = mutableMapOf<String, String>()
    val params = mutableMapOf<String, String>()

    init {
        title = "Transaction Parameters"
        configurationName = "Run ${scriptFunction.fqName}"
        selectedProfile = if (profiles.contains("default")) "default" else profiles[0]
        init()
    }

    override fun createCenterPanel(): JComponent {
        val cacheService = scriptFunction.project.service<RunTransactionCacheService>()
        return panel {
            row("Run Configuration name: ") {
                textField()
                    .bindText({ configurationName }, { configurationName = it })
                    .columns(NAME_COLUMNS)
                    .horizontalAlign(HorizontalAlign.RIGHT)
            }
            val typeParameters = scriptFunction.typeParameters
            val parameters = scriptFunction.parameterBindings().drop(1)
            val itemContext = scriptFunction.module?.itemContext(false) ?: ItemContext(false)

            if (typeParameters.isNotEmpty() || parameters.isNotEmpty()) {
                separator()
            }

            if (typeParameters.isNotEmpty()) {
                group("Type Arguments") {
                    for (typeParameter in scriptFunction.typeParameters) {
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
                                .validationOnApply(validateParseErrors("Invalid type"))
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
        val project = scriptFunction.project
        // TODO: add TYPE icon
        val completionProvider = TextFieldWithAutoCompletion.StringsCompletionProvider(variants, null)
        return MoveTextFieldWithCompletion(
            project,
            "",
            completionProvider,
            scriptFunction,
        )
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
