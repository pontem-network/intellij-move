package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.MessageType
import com.intellij.ui.JBColor
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.cli.runConfigurations.aptos.run.quoted
import org.move.lang.core.psi.MvFunction
import org.move.stdext.RsResult
import org.move.utils.ui.whenItemSelectedFromUi
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel

data class MoveProjectItem(val moveProject: MoveProject) {
    override fun toString(): String {
        return "${moveProject.currentPackage.packageName} [${moveProject.contentRootPath}]"
    }
}

abstract class FunctionCallConfigurationEditorBase<T : FunctionCallConfigurationBase>(
    val project: Project,
    protected var command: String,
    protected var moveProject: MoveProject?,
    private val subCommand: String,
) :
    SettingsEditor<T>() {

    protected var functionCall: FunctionCall

    private val moveProjectComboBox: ComboBox<MoveProjectItem> = ComboBox()

    private val typeParametersLabel = JLabel("")
    private val parametersLabel = JLabel("")

    private val editParametersButton = JButton("Edit Parameters")
    private val profilesComboBox: ComboBox<Profile> = ComboBox()

    private val environmentVariablesField = EnvironmentVariablesComponent()
    protected val errorLabel = JLabel("")

    private var functionIdItems: Collection<String> = emptyList()

    init {
        project.moveProjects.allProjects.forEach {
            moveProjectComboBox.addItem(MoveProjectItem(it))
        }
        moveProjectComboBox.selectedItem = moveProject?.let { MoveProjectItem(it) }
        moveProjectComboBox.isEnabled = moveProjectComboBox.model.size > 1

        refreshMoveProjectState()

        functionCall = FunctionCall.empty(profilesComboBox.selectedItem as Profile)

        editParametersButton.addActionListener {
            val functionId = functionCall.functionId?.cmdText() ?: return@addActionListener
            val entryFunction =
                moveProject?.let { this.getFunction(it, functionId) } ?: return@addActionListener

            val parametersDialog = FunctionCallParametersDialog(
                entryFunction,
                functionCall,
            )
            val ok = parametersDialog.showAndGet()
            if (!ok) return@addActionListener

            refreshEditorState()
        }

        errorLabel.foreground = JBColor.RED
    }

    @Suppress("LeakingThis")
    private val functionIdField: TextFieldWithAutoCompletion<String> =
        textFieldWithCompletion(
            functionCall.functionId?.cmdText() ?: "",
            getFunctionCompletionVariants(project)
        )

    abstract fun getFunctionCompletionVariants(project: Project): Collection<String>

    abstract fun getFunction(moveProject: MoveProject, functionId: String): MvFunction?

    private fun generateCommand(): String {
        val result = functionCall.toAptosCommandLine(subCommand)
        return when (result) {
            is RsResult.Ok -> result.ok.joinedCommand()
            is RsResult.Err -> {
                error(
                    "Cannot generate command. " +
                            "\nErr: ${result.err.message.quoted()}, " +
                            "\nFunctionCall: ${functionCall.toString().quoted()}"
                )
            }
        }
    }

    fun getSelectedMoveProjectFromComboBox(): MoveProject? {
        return (moveProjectComboBox.model.selectedItem as? MoveProjectItem)?.moveProject
    }

    override fun resetEditorFrom(s: T) {
        this.command = s.command
        this.moveProject = s.moveProject
        refreshEditorState()
    }

    override fun applyEditorTo(s: T) {
        s.command = command
        s.moveProject = moveProject
    }

    override fun createEditor(): JComponent {
        val editorPanel = createEditorPanel()
        return DumbService.getInstance(project).wrapGently(editorPanel, this)
    }

    private fun createEditorPanel(): DialogPanel {
        val selectedMoveProject = getSelectedMoveProjectFromComboBox()
        if (selectedMoveProject == null) {
            return errorPanel("No Aptos projects found")
        }
        val parsedFunctionCall =
            FunctionCall.parseFromCommand(selectedMoveProject, command, this::getFunction)
        if (parsedFunctionCall == null) {
            return errorPanel("Cannot parse command, re-create the Run Configuration")
        }

        functionCall.functionId = parsedFunctionCall.functionId
        functionCall.profile = parsedFunctionCall.profile ?: functionCall.profile
        functionCall.typeParams = parsedFunctionCall.typeParams
        functionCall.valueParams = parsedFunctionCall.valueParams

        functionIdField.text = parsedFunctionCall.functionId?.cmdText() ?: ""
        refreshEditorState()

        val editor = this
        val editorPanel = panel {
            row { cell(errorLabel) }
            row("Project") {
                cell(moveProjectComboBox)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .columns(COLUMNS_LARGE)
                    .whenItemSelectedFromUi { item ->
                        moveProject = item.moveProject
                        refreshMoveProjectState()
                        refreshEditorState()
                    }
            }
            row("Account") {
                cell(profilesComboBox)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .whenItemSelectedFromUi {
                        functionCall.profile = it
                        refreshEditorState()
                    }
            }
            separator()
            row("FunctionId") {
                // TODO: try to change it into combo box with search over the functions,
                // TODO: then there will be no need for Apply Change button
                // TODO: change comment into "required" popup
                cell(functionIdField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                comment("(required)")
            }
            row {
                // TODO: disable button if previous function is not modified
                // TODO: disable button is profile is not set
                button("Apply FunctionId Change") {
                    val functionId = functionIdField.text.trim()
                    val profile = functionCall.profile
                    val function = editor.getFunction(selectedMoveProject, functionId)
                    if (profile == null || function == null) {
                        // TODO: show error popup requiring profile + functionId correct
                        functionCall = FunctionCall.empty(functionCall.profile)
                    } else {
                        functionCall = FunctionCall.template(profile, function)
                    }
                    refreshEditorState()
                }
            }
            row("Type Parameters:") { cell(typeParametersLabel) }
            row("Parameters:") { cell(parametersLabel) }
            row { cell(editParametersButton) }
            row(environmentVariablesField.label) {
                cell(environmentVariablesField)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
        editorPanel.registerValidators(this)
        return editorPanel
    }

    private fun errorPanel(errorMessage: String): DialogPanel {
        setErrorText(errorMessage)
        return panel {
            row { cell(errorLabel) }
        }
    }

    private fun refreshMoveProjectState() {
        val moveProject = moveProject
        if (moveProject != null) {
            val profiles = moveProject.aptosConfigYaml?.profiles.orEmpty()
            profiles.forEach { accountName ->
                val profile = Profile(accountName, moveProject)
                profilesComboBox.addItem(profile)
            }
        }
        profilesComboBox.isEnabled = profilesComboBox.model.size > 1

        functionCall = FunctionCall.empty(profilesComboBox.selectedItem as Profile)

        functionIdItems = getFunctionCompletionVariants(project)
    }

    private fun refreshEditorState() {
        fireEditorStateChanged()
        validateEditor()
        editParametersButton.isEnabled =
            functionCall.functionId != null && functionCall.functionHasParameters()

        val commandText = generateCommand()

        command = commandText

        typeParametersLabel.text =
            functionCall.typeParams.map { "${it.key}=${it.value ?: ""}" }
                .joinToString(", ")
        parametersLabel.text =
            functionCall.valueParams.map { "${it.key}=${it.value?.value ?: ""}" }
                .joinToString(", ")
    }

    private fun validateEditor() {
//        println()
//        println("function call: $functionCall")
//        println("command: $command")

        val moveProject = moveProject ?: return

        val functionId = this.cmdFunctionId()
        if (functionId == null) {
            setErrorText("FunctionId is required")
            return
        }
        val function = this.getFunction(moveProject, functionId)
        if (function == null) {
            setErrorText("Cannot resolve function from functionId")
            return
        }
        val typeParams = functionCall.typeParams.filterValues { it == null }
        if (typeParams.isNotEmpty()) {
            setErrorText("Missing required type parameters: ${typeParams.keys.joinToString()}")
            return
        }
        val valueParams = functionCall.valueParams.filterValues { it == null }
        if (valueParams.isNotEmpty()) {
            setErrorText("Missing required value parameters: ${valueParams.keys.joinToString()}")
            return
        }
        setErrorText("")
    }

    private fun setErrorText(text: String) {
        errorLabel.text = text
        errorLabel.foreground = MessageType.ERROR.titleForeground
        errorLabel.icon = if (text.isBlank()) null else AllIcons.Actions.Lightning
    }

    private fun textFieldWithCompletion(
        initialValue: String,
        variants: Collection<String>
    ): TextFieldWithAutoCompletion<String> {
        val completionProvider = TextFieldWithAutoCompletion.StringsCompletionProvider(variants, null)
        val textField = TextFieldWithAutoCompletion(
            project,
            completionProvider,
            false,
            initialValue
        )
        return textField
    }

    private fun cmdFunctionId(): String? = functionCall.functionId?.cmdText()
}
