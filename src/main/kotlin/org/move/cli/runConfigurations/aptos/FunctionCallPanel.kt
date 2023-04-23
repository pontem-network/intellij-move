package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.components.BorderLayoutPanel
import org.move.cli.MoveProject
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyInteger
import org.move.utils.ui.CompletionTextField
import org.move.utils.ui.MoveTextFieldWithCompletion
import org.move.utils.ui.ulongTextField
import org.move.utils.ui.whenTextChangedFromUi
import javax.swing.JButton
import javax.swing.JPanel

typealias TypeParamsMap = MutableMap<String, String?>
typealias ValueParamsMap = MutableMap<String, FunctionCallParam?>

class TypeParameterTextField(
    project: Project,
    item: MvFunction,
    initialValue: String,
    completionVariants: Collection<String>,
) :
    MoveTextFieldWithCompletion(
        project,
        initialValue,
        TextFieldWithAutoCompletion.StringsCompletionProvider(completionVariants, null),
        item
    )

class FunctionCallPanel(
    val handler: FunctionCallConfigurationHandler,
    var moveProject: MoveProject,
) :
    BorderLayoutPanel() {

    private lateinit var item: MvFunction
    private lateinit var typeParams: TypeParamsMap
    private lateinit var valueParams: MutableMap<String, FunctionCallParam?>

    private val functionCompletionField = CompletionTextField(project, "", emptyList())
    private val saveFunctionButton = JButton("Apply")

    val project get() = moveProject.project

    init {
        val panel = this
        functionCompletionField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val oldFunctionName = panel.item.qualName?.editorText()
                val newFunctionName = event.document.text
                saveFunctionButton.isEnabled =
                    newFunctionName != oldFunctionName
                            && handler.getFunction(moveProject, newFunctionName) != null
            }
        })
        saveFunctionButton.addActionListener {
            val itemName = functionCompletionField.text
            val function = handler.getFunction(moveProject, itemName)
                ?: error("Button should be disabled if function name is invalid")
            val functionCall = function.instantiateCall()
            this.updateFromFunctionCall(functionCall)
            fireChangeEvent()
        }
    }

    fun updateFromFunctionCall(functionCall: FunctionCall) {
        this.item = functionCall.item
        this.typeParams = functionCall.typeParams
        this.valueParams = functionCall.valueParams

        this.functionCompletionField.text = functionCall.itemName() ?: ""
        val completionVariants = handler.getFunctionCompletionVariants(moveProject)
        this.functionCompletionField.setVariants(completionVariants)

        recreateInnerPanel()
    }

    fun reset(moveProject: MoveProject) {
        this.moveProject = moveProject
        val variants = handler.getFunctionCompletionVariants(moveProject)

        this.item = variants.firstOrNull()?.let { handler.getFunction(this.moveProject, it) }
            ?: error("Should always have one valid function")

        val functionCall = this.item.instantiateCall()
        updateFromFunctionCall(functionCall)
    }

    private fun recreateInnerPanel() {
        removeAll()
        addToCenter(getInnerPanel())
        validate()
    }

    interface FunctionCallListener {
        fun functionCallChanged(functionCall: FunctionCall)
    }

    private val eventListeners = mutableListOf<FunctionCallListener>()

    fun addFunctionCallListener(listener: FunctionCallListener) {
        eventListeners.add(listener)
    }

    private fun fireChangeEvent() {
        val functionCall = FunctionCall(this.item, this.typeParams, this.valueParams)
        eventListeners.forEach {
            it.functionCallChanged(functionCall)
        }
    }

    private fun getInnerPanel(): JPanel {
        val function = this.item
        val cacheService = project.service<RunTransactionCacheService>()
        val thisPanel = this
        return panel {
            val typeParameters = function.typeParameters
            val parameters = function.transactionParameters.map { it.bindingPat }
            val msl = false
            val inference = function.inference(msl)

            row("Function (required)") {
                cell(functionCompletionField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                cell(saveFunctionButton)
            }

            if (typeParameters.isNotEmpty()) {
                group("Type Arguments") {
                    for (typeParameter in function.typeParameters) {
                        val paramName = typeParameter.name ?: continue
                        row(paramName) {
                            val completionVariants = cacheService.getTypeParameterCache(paramName)
                            val initialValue = thisPanel.typeParams[paramName] ?: ""
                            val typeParameterTextField = TypeParameterTextField(
                                project,
                                function,
                                initialValue,
                                completionVariants
                            )
                            typeParameterTextField.addDocumentListener(object : DocumentListener {
                                override fun documentChanged(event: DocumentEvent) {
                                    thisPanel.typeParams[paramName] = event.document.text
                                    fireChangeEvent()
                                }
                            })
                            cell(typeParameterTextField)
                                .horizontalAlign(HorizontalAlign.FILL)
//                                .bindText(
//                                    { functionCall.typeParams[paramName] ?: "" },
//                                    { functionCall.typeParams[paramName] = it })
//                                .registerValidationRequestor()
//                                .validationOnApply(validateEditorTextNonEmpty("Required type parameter"))
//                                .validationOnApply(validateParseErrors("Invalid type format"))
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
                                .horizontalAlign(HorizontalAlign.FILL)
                                .whenTextChangedFromUi {
                                    thisPanel.valueParams[paramName] = FunctionCallParam(it, paramTyName)
                                    fireChangeEvent()
                                }
                                .bindText({ thisPanel.valueParams[paramName]?.value ?: "" }, {})
//                            paramField
//                                .bindText(
//                                    { thisPanel.valueParams[paramName]?.value ?: "" },
//                                    {
////                                        if (it.isNotBlank()) {
////                                            thisPanel.valueParams[paramName] =
////                                                FunctionCallParam(it, paramTyName)
////                                            fireChangeEvent()
////                                        }
//                                    })
//                                .horizontalAlign(HorizontalAlign.FILL)
////                                .validationOnApply(validateNonEmpty("Required parameter"))

                        }
                    }
                }
            }
        }
    }
}

fun MvFunction.instantiateCall(): FunctionCall = FunctionCall.template(this)
