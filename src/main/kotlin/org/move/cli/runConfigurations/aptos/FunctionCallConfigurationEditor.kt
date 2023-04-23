package org.move.cli.runConfigurations.aptos

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.stdext.RsResult
import org.move.utils.ui.whenItemSelectedFromUi
import javax.swing.JComponent
import javax.swing.JLabel

data class MoveProjectItem(val moveProject: MoveProject) {
    override fun toString(): String {
        return "${moveProject.currentPackage.packageName} [${moveProject.contentRootPath}]"
    }
}

class FunctionCallConfigurationEditor<T : FunctionCallConfigurationBase>(
    private val handler: FunctionCallConfigurationHandler,
    private var moveProject: MoveProject,
) :
    SettingsEditor<T>() {

    private val project = moveProject.project

    private var signerAccount: String? = null
    private var functionCall: FunctionCall? = null

    private val projectComboBox: ComboBox<MoveProjectItem> = ComboBox()
    private val accountComboBox: ComboBox<String> = ComboBox()

    private val functionCallPanel = FunctionCallPanel(handler, moveProject)

    private val errorLabel = JLabel("")

    private lateinit var editorPanel: DialogPanel

    init {
        errorLabel.foreground = JBColor.RED

        moveProject.project.moveProjects.allProjects.forEach {
            projectComboBox.addItem(MoveProjectItem(it))
        }
        projectComboBox.isEnabled = projectComboBox.model.size > 1
        projectComboBox.selectedItem = MoveProjectItem(moveProject)

        fillAccountsComboBox()

        val editor = this
        functionCallPanel.addFunctionCallListener(object : FunctionCallPanel.FunctionCallListener {
            override fun functionCallChanged(functionCall: FunctionCall) {
                editor.functionCall = functionCall
            }
        })
        functionCallPanel.reset(moveProject)
    }

    override fun resetEditorFrom(s: T) {
        val moveProject = s.workingDirectory?.let { project.moveProjects.findMoveProject(it) }
        if (moveProject == null) {
            setErrorText("Deserialization error: no Aptos project found in the specified working directory")
            editorPanel.isVisible = false
            this.signerAccount = null
            this.functionCall = null
            return
        }
        fillAccountsComboBox()

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
        this.accountComboBox.selectedItem = profile

        functionCallPanel.updateFromFunctionCall(functionCall)
    }

    override fun applyEditorTo(s: T) {
        val moveProject = moveProject
        val profile = signerAccount
        val functionCall = functionCall

        s.moveProject = moveProject
        if (profile != null && functionCall != null) {
            s.command =
                handler.generateCommand(moveProject, profile, functionCall).unwrapOrNull() ?: ""
        } else {
            s.command = ""
        }
    }

    override fun disposeEditor() {
        Disposer.dispose(functionCallPanel)
    }

    override fun createEditor(): JComponent {
        editorPanel = createEditorPanel()
        val outerPanel = panel {
            row { cell(errorLabel) }
            row {
                cell(editorPanel)
                    .verticalAlign(VerticalAlign.FILL)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
        return DumbService.getInstance(project).wrapGently(outerPanel, this)
    }

    private fun createEditorPanel(): DialogPanel {
        val editorPanel = panel {
            row { cell(errorLabel) }
            row("Project") {
                cell(projectComboBox)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .columns(COLUMNS_LARGE)
                    .whenItemSelectedFromUi {
                        moveProject = it.moveProject
                        fillAccountsComboBox()
                        functionCallPanel.reset(moveProject)
                    }
            }
            row("Account") {
                cell(accountComboBox)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .whenItemSelectedFromUi {
                        signerAccount = it
                    }
            }
            separator()
            row {
                cell(functionCallPanel)
                    .verticalAlign(VerticalAlign.FILL)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
        editorPanel.registerValidators(this)
        return editorPanel
    }

    fun fillAccountsComboBox() {
        accountComboBox.removeAllItems()
        val accounts = moveProject.aptosConfigYaml?.profiles.orEmpty()
        accounts.forEach { accountName ->
            accountComboBox.addItem(accountName)
        }
        accountComboBox.isEnabled = accountComboBox.model.size > 1
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
