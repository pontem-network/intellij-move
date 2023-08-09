package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.components.BorderLayoutPanel
import org.move.cli.MoveProject
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyInteger
import org.move.utils.ui.*
import java.util.function.Supplier
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
    ) {
    fun addDefaultValidator(parentDisposable: Disposable): ComponentValidator {
        val field = this
        return ComponentValidator(parentDisposable)
            .withValidator(Supplier {
                if (field.text.isEmpty()) return@Supplier ValidationInfo("Required", field)
                if (field.hasParseErrors()) return@Supplier ValidationInfo("Invalid type format", field)
                null
            })
            .installOn(field)
            .andRegisterOnDocumentListener(field)
    }
}

class FunctionCallPanel(
    val handler: CommandConfigurationHandler,
    var moveProject: MoveProject,
) :
    BorderLayoutPanel(), Disposable {

    private lateinit var item: MvFunction
    private lateinit var typeParams: TypeParamsMap
    private lateinit var valueParams: MutableMap<String, FunctionCallParam?>

    private val functionCompletionField = CompletionTextField(project, "", emptyList())
    private val functionApplyButton = JButton("Apply")

    private val functionValidator: ComponentValidator

    val project get() = moveProject.project

    init {
        val panel = this

        functionCompletionField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val oldFunctionName = panel.item.qualName?.editorText()
                val newFunctionName = event.document.text
                functionApplyButton.isEnabled =
                    newFunctionName != oldFunctionName
                            && handler.getFunction(moveProject, newFunctionName) != null
            }
        })
        functionApplyButton.addActionListener {
            val itemName = functionCompletionField.text
            val function = handler.getFunction(moveProject, itemName)
                ?: error("Button should be disabled if function name is invalid")
            val functionCall = function.instantiateCall()
            this.updateFromFunctionCall(functionCall)
            fireChangeEvent()
        }

        functionValidator = ComponentValidator(panel)
            .withValidator(Supplier<ValidationInfo?> {
                val text = functionCompletionField.text
                if (text.isBlank()) return@Supplier ValidationInfo("Required", functionCompletionField)
                val function = handler.getFunction(moveProject, text)
                if (function == null) {
                    return@Supplier ValidationInfo("Invalid entry function", functionCompletionField)
                }
                null
            })
            .andRegisterOnDocumentListener(functionCompletionField)
            .installOn(functionCompletionField)
    }

    override fun dispose() {
    }

    fun updateFromFunctionCall(functionCall: FunctionCall) {
        this.item = functionCall.item
        this.typeParams = functionCall.typeParams
        this.valueParams = functionCall.valueParams

        this.functionCompletionField.text = functionCall.itemName() ?: ""
        val completionVariants = handler.getFunctionCompletionVariants(moveProject)
        this.functionCompletionField.setVariants(completionVariants)

        recreateInnerPanel()
        functionValidator.revalidate()
    }

    fun reset(moveProject: MoveProject) {
        this.moveProject = moveProject
        val variants = handler.getFunctionCompletionVariants(moveProject)

        this.item = variants.firstOrNull()
            ?.let { handler.getFunction(this.moveProject, it) }
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
        val outerPanel = this
        return panel {
            val typeParameters = function.typeParameters
            val parameters = handler.getFunctionParameters(function).map { it.bindingPat }

            row("Function") {
                cell(functionCompletionField)
                    .align(AlignX.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                cell(functionApplyButton)
            }
            if (typeParameters.isNotEmpty() || parameters.isNotEmpty()) {
                separator()
            }
            if (typeParameters.isNotEmpty()) {
                val cacheService = project.service<RunTransactionCacheService>()
                for (typeParameter in function.typeParameters) {
                    val paramName = typeParameter.name ?: continue
                    row(paramName) {
                        val completionVariants = cacheService.getTypeParameterCache(paramName)
                        val initialValue = outerPanel.typeParams[paramName] ?: ""

                        val typeParameterTextField = TypeParameterTextField(
                            project,
                            function,
                            initialValue,
                            completionVariants
                        )
                        typeParameterTextField
                            .addDefaultValidator(outerPanel)
                            .revalidate()
                        typeParameterTextField.addDocumentListener(object : DocumentListener {
                            override fun documentChanged(event: DocumentEvent) {
                                outerPanel.typeParams[paramName] = event.document.text
                                fireChangeEvent()
                            }
                        })
                        cell(typeParameterTextField)
                            .align(AlignX.FILL)
//                            .horizontalAlign(HorizontalAlign.FILL)
                    }
                }
            }
            if (parameters.isNotEmpty()) {
                val msl = false
                val inference = function.inference(msl)
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
                        paramField.component.text = outerPanel.valueParams[paramName]?.value
                        paramField
                            .align(AlignX.FILL)
//                            .horizontalAlign(HorizontalAlign.FILL)
                            .whenTextChangedFromUi {
                                outerPanel.valueParams[paramName] = FunctionCallParam(it, paramTyName)
                                fireChangeEvent()
                            }

                    }
                }
            }
        }
    }
}

fun MvFunction.instantiateCall(): FunctionCall = FunctionCall.template(this)
