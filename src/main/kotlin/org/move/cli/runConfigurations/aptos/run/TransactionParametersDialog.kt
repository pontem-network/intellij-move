package org.move.cli.runConfigurations.aptos.run

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.move.cli.transactions.RunTransactionCacheService
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.ty.TyAddress
import org.move.lang.core.types.ty.TyBool
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyVector
import org.move.utils.ui.*
import javax.swing.JComponent

class TransactionParametersDialog(
    val entryFunction: MvFunction,
    val transaction: Transaction,
) : DialogWrapper(entryFunction.project, false) {

    init {
        title = "Transaction Parameters"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val project = entryFunction.project
        val cacheService = project.service<RunTransactionCacheService>()

        return panel {
            val typeParameters = entryFunction.typeParameters
            val parameters = entryFunction.transactionParameters.map { it.bindingPat }
            val itemContext =
                entryFunction.module?.itemContext(false) ?: project.itemContext(false)

//            if (typeParameters.isNotEmpty() || parameterBindings.isNotEmpty()) {
//                separator()
//            }

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
                                    { transaction.typeParams[paramName] ?: "" },
                                    { transaction.typeParams[paramName] = it })
                                .registerValidationRequestor()
                                .validationOnApply(validateEditorTextNonEmpty("Required type parameter"))
                                .validationOnApply(validateParseErrors("Invalid type format"))
                        }
                    }
                }
            }
            if (parameters.isNotEmpty()) {
                val inferenceCtx = InferenceContext(false, itemContext)
                group("Value Arguments") {
                    for (parameter in parameters) {
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
                                    { transaction.params[paramName] ?: "" },
                                    {
                                        if (it.isNotBlank()) {
                                            transaction.params[paramName] = "$paramTyName:$it"
                                        }
                                    })
//                                .columns(ARGUMENT_COLUMNS)
                                .horizontalAlign(HorizontalAlign.FILL)
                                .validationOnApply(validateNonEmpty("Required parameter"))

                        }
                    }
                }
            }
        }
    }

    private fun typeParameterTextField(variants: Collection<String>): LanguageTextField {
        val project = entryFunction.project
        val completionProvider = TextFieldWithAutoCompletion.StringsCompletionProvider(variants, null)
        return MoveTextFieldWithCompletion(
            project,
            "",
            completionProvider,
            entryFunction,
        )
    }
}
