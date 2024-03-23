package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.Disposable
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
//typealias ValueParamsMap = MutableMap<String, FunctionCallParam?>

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

interface FunctionParameterPanelListener {
    fun functionParametersChanged(functionCall: FunctionCall)
}

/**
 * Panel that covers UI elements of transaction run / view Run Configurations
 * Consists of:
 * - function name (with completion variants)
 * - type parameters
 * - parameters
 */
class FunctionParametersPanel(
    val commandHandler: CommandConfigurationHandler,
    var moveProject: MoveProject,
) :
    BorderLayoutPanel(), Disposable {

    private lateinit var functionItem: MvFunction
    private lateinit var typeParams: TypeParamsMap
    private lateinit var valueParams: MutableMap<String, FunctionCallParam?>

    private val functionItemField = CompletionTextField(project, "", emptyList())
    private val functionApplyButton = JButton("Select function")

    private val functionValidator: ComponentValidator

    val project get() = moveProject.project

    init {
        val panel = this

        // enables Apply button if function name is changed and valid
        functionItemField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val oldFunctionName = panel.functionItem.qualName?.editorText()
                val newFunctionName = event.document.text
                functionApplyButton.isEnabled =
                    newFunctionName != oldFunctionName
                            && commandHandler.getFunctionItem(moveProject, newFunctionName) != null
            }
        })

        // update type parameters form and value parameters form on "Apply" button click
        functionApplyButton.addActionListener {
            val itemName = functionItemField.text
            val functionItem = commandHandler.getFunctionItem(moveProject, itemName)
                ?: error("Button should be disabled if function name is invalid")
            val functionCall = functionItem.newFunctionCall()
            this.updateFromFunctionCall(functionCall)
            fireChangeEvent()
        }

        // validates
        this.functionValidator = ComponentValidator(panel)
            .withValidator(Supplier<ValidationInfo?> {
                val text = functionItemField.text
                if (text.isBlank()) return@Supplier ValidationInfo("Required", functionItemField)
                val functionItem = commandHandler.getFunctionItem(moveProject, text)
                if (functionItem == null) {
                    return@Supplier ValidationInfo("Invalid entry function", functionItemField)
                }
                null
            })
            .andRegisterOnDocumentListener(functionItemField)
            .installOn(functionItemField)
    }

    override fun dispose() {
    }

    fun updateFromFunctionCall(functionCall: FunctionCall) {
        this.functionItem = functionCall.item
        this.typeParams = functionCall.typeParams
        this.valueParams = functionCall.valueParams

        this.functionItemField.text = functionCall.itemName() ?: ""

        val completionVariants = commandHandler.getFunctionCompletionVariants(moveProject)
        this.functionItemField.setVariants(completionVariants)

        recreateInnerPanel()
        functionValidator.revalidate()
    }

    fun setMoveProjectAndCompletionVariants(moveProject: MoveProject) {
        this.moveProject = moveProject

        val variants = commandHandler.getFunctionCompletionVariants(moveProject)
        this.functionItem = variants.firstOrNull()
            ?.let { commandHandler.getFunctionItem(this.moveProject, it) }
            ?: error("Should always have one valid function")

        val functionCall = this.functionItem.newFunctionCall()
        updateFromFunctionCall(functionCall)
    }

    private fun recreateInnerPanel() {
        removeAll()
        addToCenter(getInnerPanel())
        validate()
    }

    private val eventListeners = mutableListOf<FunctionParameterPanelListener>()

    fun addFunctionCallListener(listener: FunctionParameterPanelListener) {
        eventListeners.add(listener)
    }

    fun fireChangeEvent() {
        val functionCall = FunctionCall(this.functionItem, this.typeParams, this.valueParams)
        eventListeners.forEach {
            it.functionParametersChanged(functionCall)
        }
    }

    private fun getInnerPanel(): JPanel {
        val function = this.functionItem
        val outerPanel = this
        return panel {
            val typeParameters = function.typeParameters
            val parameters = commandHandler.getFunctionParameters(function).map { it.bindingPat }

            row("Function") {
                cell(functionItemField)
                    .align(AlignX.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                cell(functionApplyButton)
            }
            if (typeParameters.isNotEmpty() || parameters.isNotEmpty()) {
                separator()
            }
            if (typeParameters.isNotEmpty()) {
                for (typeParameter in function.typeParameters) {
                    val paramName = typeParameter.name ?: continue
                    row(paramName) {
                        val initialValue = outerPanel.typeParams[paramName] ?: ""

                        val typeParameterTextField = TypeParameterTextField(
                            project,
                            function,
                            initialValue,
                            emptyList()
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

fun MvFunction.newFunctionCall(): FunctionCall = FunctionCall.template(this)
