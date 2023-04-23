package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyInteger
import org.move.utils.ui.*
import javax.swing.JComponent

class FunctionCallParametersDialog(val functionCall: FunctionCall) :
    DialogWrapper(functionCall.item.project, false, IdeModalityType.PROJECT) {

    init {
        title = "Specify Function Parameters"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val function = functionCall.item
        val project = function.project
        val cacheService = project.service<RunTransactionCacheService>()

        return panel {
            val typeParameters = function.typeParameters
            val parameters = function.transactionParameters.map { it.bindingPat }
            val msl = false
            val inference = function.inference(msl)
            if (typeParameters.isNotEmpty()) {
                group("Type Arguments") {
                    for (typeParameter in function.typeParameters) {
                        val paramName = typeParameter.name ?: continue
                        row(paramName) {
                            val previousValues = cacheService.getTypeParameterCache(paramName)
                            cell(typeParameterTextField(previousValues))
                                .horizontalAlign(HorizontalAlign.FILL)
                                .bindText(
                                    { functionCall.typeParams[paramName] ?: "" },
                                    { functionCall.typeParams[paramName] = it })
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
                        val paramName = parameter.name
                        val paramTy = inference.getPatType(parameter)
                        val paramTyName = FunctionCallParam.tyTypeName(paramTy)
                        row(paramName) {
                            comment(": $paramTyName")
                            val paramField = when (paramTy) {
                                is TyInteger -> ulongTextField(paramTy.ulongRange())
                                else -> textField()
                            }
                            paramField
                                .bindText(
                                    { functionCall.valueParams[paramName]?.value ?: "" },
                                    {
                                        if (it.isNotBlank()) {
                                            functionCall.valueParams[paramName] =
                                                FunctionCallParam(it, paramTyName)
                                        }
                                    })
                                .horizontalAlign(HorizontalAlign.FILL)
                                .validationOnApply(validateNonEmpty("Required parameter"))

                        }
                    }
                }
            }
        }
    }

    private fun typeParameterTextField(variants: Collection<String>): LanguageTextField {
        val function = functionCall.item
        val project = function.project
        val completionProvider = TextFieldWithAutoCompletion.StringsCompletionProvider(variants, null)
        return MoveTextFieldWithCompletion(
            project,
            "",
            completionProvider,
            function,
        )
    }
}

fun typeParameterTextField(functionCall: FunctionCall, variants: Collection<String>): LanguageTextField {
    val function = functionCall.item
    val project = function.project
    val completionProvider = TextFieldWithAutoCompletion.StringsCompletionProvider(variants, null)
    return MoveTextFieldWithCompletion(
        project,
        "",
        completionProvider,
        function,
    )
}
