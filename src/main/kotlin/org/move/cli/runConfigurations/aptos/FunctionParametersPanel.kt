package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.util.ui.components.BorderLayoutPanel
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyInteger
import org.move.utils.ui.MoveTextFieldWithCompletion
import org.move.utils.ui.hasParseErrors
import org.move.utils.ui.ulongTextField
import java.util.function.Supplier
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
    val project: Project,
    val commandHandler: CommandConfigurationHandler,
) :
    BorderLayoutPanel(), Disposable {

    var functionItem: SmartPsiElementPointer<MvFunction>? = null

    private var typeParams: TypeParamsMap = mutableMapOf()
    private var valueParams: ValueParamsMap = mutableMapOf()

    fun updateFromFunctionCall(functionCall: FunctionCall) {
        this.functionItem = functionCall.item
        this.typeParams = functionCall.typeParams
        this.valueParams = functionCall.valueParams

        recreateInnerPanel()
    }

    fun clear() {
        this.functionItem = null
        this.typeParams = mutableMapOf()
        this.valueParams = mutableMapOf()
    }

    override fun dispose() {}

    private fun recreateInnerPanel() {
        removeAll()
        addToCenter(createInnerPanel())
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

    private fun createInnerPanel(): JPanel {
        val function = this.functionItem?.element
        val outerPanel = this
        return panel {
            val typeParameters = function?.typeParameters.orEmpty()
            val parameterBindings = function
                ?.let { commandHandler.getFunctionParameters(function).map { it.bindingPat } }
                .orEmpty()

            if (typeParameters.isNotEmpty()) {
                for (typeParameter in typeParameters) {
                    val paramName = typeParameter.name ?: continue
                    row(paramName) {
                        val initialValue = outerPanel.typeParams[paramName] ?: ""

                        val typeParameterTextField = TypeParameterTextField(
                            project,
                            function!!,
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
                        cell(typeParameterTextField).align(AlignX.FILL)
                    }
                }
            }
            if (parameterBindings.isNotEmpty()) {
                val msl = false
                val inference = function!!.inference(msl)
                for (parameterBinding in parameterBindings) {
                    val paramName = parameterBinding.name
                    val paramTy = inference.getBindingType(parameterBinding)
                    val paramTyName = FunctionCallParam.tyTypeName(paramTy)
                    row(paramName) {
                        comment(": $paramTyName")
                        val paramField = when (paramTy) {
                            is TyInteger -> ulongTextField(paramTy.ulongRange())
                            else -> textField()
                        }
                        paramField.component.text = outerPanel.valueParams[paramName]?.value
                        @Suppress("UnstableApiUsage")
                        paramField
                            .align(AlignX.FILL)
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
