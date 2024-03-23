package org.move.cli.runConfigurations.aptos

import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.*
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService
import org.move.stdext.RsResult
import org.move.utils.ui.whenItemSelectedFromUi
import org.move.utils.ui.whenTextChangedFromUi
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextField

data class MoveProjectItem(val moveProject: MoveProject) {
    override fun toString(): String {
        return "${moveProject.currentPackage.packageName} [${moveProject.contentRootPath}]"
    }
}

class FunctionCallConfigurationEditor<T : FunctionCallConfigurationBase>(
    private val handler: CommandConfigurationHandler,
    private var moveProject: MoveProject,
) :
    SettingsEditor<T>() {

    private val project = moveProject.project

    private var signerAccount: String? = null
    private var functionCall: FunctionCall? = null

    private val projectComboBox: ComboBox<MoveProjectItem> = ComboBox()
    private val accountTextField = JTextField()

    private val functionParametersPanel = FunctionParametersPanel(handler, moveProject)

    private val errorLabel = JLabel("")

    private lateinit var editorPanel: DialogPanel

    init {
        errorLabel.foreground = JBColor.RED

        moveProject.project.moveProjectsService.allProjects.forEach {
            projectComboBox.addItem(MoveProjectItem(it))
        }
        projectComboBox.isEnabled = projectComboBox.model.size > 1
        projectComboBox.selectedItem = MoveProjectItem(moveProject)

        val editor = this
        functionParametersPanel.addFunctionCallListener(object : FunctionParameterPanelListener {
            override fun functionParametersChanged(functionCall: FunctionCall) {
                editor.functionCall = functionCall
            }
        })
        functionParametersPanel.setMoveProjectAndCompletionVariants(moveProject)
    }

    override fun resetEditorFrom(s: T) {
        val moveProject = s.workingDirectory?.let { project.moveProjectsService.findMoveProjectForPath(it) }
        if (moveProject == null) {
            setErrorText("Deserialization error: no Aptos project found in the specified working directory")
            editorPanel.isVisible = false
            this.signerAccount = null
            this.functionCall = null
            return
        }

        val res = handler.parseCommand(moveProject, s.command)
        val (profile, functionCall) = when (res) {
            is RsResult.Ok -> res.ok
            is RsResult.Err -> {
                setErrorText("Deserialization error: ${res.err}")
                editorPanel.isVisible = false
                signerAccount = null
                functionCall = null
                return
            }
        }
        this.signerAccount = profile
        this.accountTextField.text = profile

        functionParametersPanel.updateFromFunctionCall(functionCall)
    }

    override fun applyEditorTo(s: T) {
        functionParametersPanel.fireChangeEvent()
        val moveProject = moveProject
        val profile = signerAccount
        val functionCall = functionCall

        s.moveProjectFromWorkingDirectory = moveProject
        if (profile != null && functionCall != null) {
            s.command =
                handler.generateCommand(moveProject, profile, functionCall).unwrapOrNull() ?: ""
        } else {
            s.command = ""
        }

        println("Command in applyEditorTo = ${s.command}")
    }

    override fun disposeEditor() {
        Disposer.dispose(functionParametersPanel)
    }

    override fun createEditor(): JComponent {
        editorPanel = createEditorPanel()
        val outerPanel = panel {
            row { cell(errorLabel) }
            row {
                cell(editorPanel)
                    .align(AlignX.FILL + AlignY.FILL)
//                    .verticalAlign(VerticalAlign.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
        return DumbService.getInstance(project).wrapGently(outerPanel, this)
    }

    private fun createEditorPanel(): DialogPanel {
        val editorPanel = panel {
            row { cell(errorLabel) }
            row("Project") {
                cell(projectComboBox)
                    .align(AlignX.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
                    .columns(COLUMNS_LARGE)
                    .whenItemSelectedFromUi {
                        moveProject = it.moveProject
                        functionParametersPanel.setMoveProjectAndCompletionVariants(moveProject)
                    }
            }
            row("Account") {
                cell(accountTextField)
                    .align(AlignX.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
                    .whenTextChangedFromUi {
                        signerAccount = it
                    }
            }
            separator()
            row {
                cell(functionParametersPanel)
                    .align(AlignX.FILL + AlignY.FILL)
//                    .verticalAlign(VerticalAlign.FILL)
//                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
        editorPanel.registerValidators(this)
        return editorPanel
    }

//    private fun validateEditor() {
//        val functionCall = this.functionCall
//        if (functionCall == null) {
//            setErrorText("FunctionId is required")
//            return
//        }
//        val functionItemName = functionCall.itemName()
//        if (functionItemName == null) {
//            setErrorText("FunctionId is required")
//            return
//        }
//        val function = handler.getFunction(moveProject, functionItemName)
//        if (function == null) {
//            setErrorText("Cannot resolve function from functionId")
//            return
//        }
//        val typeParams = functionCall.typeParams.filterValues { it == null }
//        if (typeParams.isNotEmpty()) {
//            setErrorText("Missing required type parameters: ${typeParams.keys.joinToString()}")
//            return
//        }
//        val valueParams = functionCall.valueParams.filterValues { it == null }
//        if (valueParams.isNotEmpty()) {
//            setErrorText("Missing required value parameters: ${valueParams.keys.joinToString()}")
//            return
//        }
//        setErrorText("")
//    }

    private fun setErrorText(text: String) {
        errorLabel.text = text
        errorLabel.foreground = MessageType.ERROR.titleForeground
        errorLabel.icon = if (text.isBlank()) null else AllIcons.Actions.Lightning
    }
}
