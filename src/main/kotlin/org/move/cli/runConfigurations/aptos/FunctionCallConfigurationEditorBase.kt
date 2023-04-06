package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.MessageType
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.move.cli.moveProjects
import org.move.lang.core.psi.MvFunction
import org.move.lang.index.MvEntryFunctionIndex
import org.move.utils.ui.whenItemSelectedFromUi
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel

abstract class FunctionCallConfigurationEditorBase<T : FunctionCallConfigurationBase>(
    val project: Project,
    protected var command: String,
    protected var workingDirectory: Path?,
) :
    SettingsEditor<T>() {

    protected var functionCall: FunctionCall

    private val commandTextField = JBTextField("")
    private val environmentVariablesField = EnvironmentVariablesComponent()

    private val typeParametersLabel = JLabel("")
    private val parametersLabel = JLabel("")

    private val editParametersButton = JButton("Edit Parameters")
    private val profilesComboBox: ComboBox<Profile> = ComboBox()

    protected val errorLabel = JLabel("")

    init {
        commandTextField.isEditable = false

        project.moveProjects.allProjects
            .forEach { proj ->
                val projectProfiles = proj.aptosConfigYaml?.profiles.orEmpty()
                projectProfiles.forEach { profileName ->
                    val profile = Profile(profileName, proj)
                    profilesComboBox.addItem(profile)
                }
            }
        functionCall = FunctionCall.empty(profilesComboBox.selectedItem as Profile)

        editParametersButton.addActionListener {
            val functionId = functionCall.functionId?.cmdText() ?: return@addActionListener
            val entryFunction = this.getFunction(project, functionId)
            if (entryFunction == null) return@addActionListener

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

    private val functionIdField: TextFieldWithAutoCompletion<String> =
        textFieldWithCompletion(
            functionCall.functionId?.cmdText() ?: "",
            getFunctionCompletionVariants(project)
        )

    abstract fun getFunctionCompletionVariants(project: Project): Collection<String>

    abstract fun getFunction(project: Project, functionId: String): MvFunction?

    abstract fun generateCommand(): String

    override fun resetEditorFrom(s: T) {
        this.command = s.command
        this.workingDirectory = s.workingDirectory
    }

    override fun applyEditorTo(s: T) {
        s.command = command
        s.workingDirectory = workingDirectory
    }

    override fun createEditor(): JComponent {
        val editorPanel = createEditorPanel()
        return DumbService.getInstance(project).wrapGently(editorPanel, this)
    }

    private fun createEditorPanel(): DialogPanel {
        val parsedFunctionCall =
            FunctionCall.parseFromCommand(project, command, workingDirectory, this::getFunction)
        if (parsedFunctionCall == null) {
            setErrorText("Cannot parse serialized command")
            return panel {
                row { cell(errorLabel) }
            }
        }

        functionCall.functionId = parsedFunctionCall.functionId
        functionCall.profile = parsedFunctionCall.profile ?: functionCall.profile
        functionCall.typeParams = parsedFunctionCall.typeParams
        functionCall.params = parsedFunctionCall.params

        functionIdField.text = parsedFunctionCall.functionId?.cmdText() ?: ""
        refreshEditorState()

        val editorPanel = panel {
            row("Command (generated):") {
                cell(commandTextField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .columns(COLUMNS_LARGE)
            }
            separator()
            row("Profile:") {
                cell(profilesComboBox)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                    .whenItemSelectedFromUi {
                        functionCall.profile = it
                        refreshEditorState()
                    }
            }
            row("Function:") {
                // TODO: try to change it into combo box with search over the functions,
                // TODO: then there will be no need for Apply Change button
                // TODO: change comment into "required" popup
                cell(functionIdField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                comment("(required)")
            }
            row {
                // TODO: validate if function id is not selected or invalid
                // TODO: disable button if previous function is not modified
                // TODO: disable button is profile is not set
                button("Apply FunctionId Change") {
                    val functionId = functionIdField.text.trim()
                    val profile = functionCall.profile
                    val entryFunction = MvEntryFunctionIndex.getEntryFunction(project, functionId)
                    if (profile == null || entryFunction == null) {
                        // TODO: show error popup requiring profile + functionId correct
                        functionCall = FunctionCall.empty(functionCall.profile)
                    } else {
                        functionCall = FunctionCall.template(profile, entryFunction)
                    }
                    refreshEditorState()
                }
            }
            row("Type Parameters:") { cell(typeParametersLabel) }
            row("Parameters:") { cell(parametersLabel) }
            row { cell(editParametersButton) }
            separator()
            row(environmentVariablesField.label) {
                cell(environmentVariablesField)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
            row { cell(errorLabel) }
        }
        editorPanel.registerValidators(this)
        return editorPanel
    }

    private fun refreshEditorState() {
        fireEditorStateChanged()
        validateEditor()
        editParametersButton.isEnabled =
            functionCall.functionId != null && functionCall.hasRequiredParameters()

        val commandText = generateCommand()

        command = commandText
        workingDirectory = functionCall.profile?.moveProject?.contentRootPath

        commandTextField.text = commandText
        typeParametersLabel.text =
            functionCall.typeParams.map { "${it.key}=${it.value ?: ""}" }
                .joinToString(", ")
        parametersLabel.text =
            functionCall.params.map { "${it.key}=${it.value?.value ?: ""}" }
                .joinToString(", ")
    }

    private fun validateEditor() {
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
}
