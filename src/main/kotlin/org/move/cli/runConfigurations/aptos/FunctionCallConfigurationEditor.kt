package org.move.cli.runConfigurations.aptos

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.*
import com.intellij.xdebugger.impl.ui.TextViewer
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService
import org.move.stdext.RsResult
import org.move.utils.ui.CompletionTextField
import java.util.function.Supplier
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextField

data class MoveProjectItem(val moveProject: MoveProject) {
    override fun toString(): String {
        return "${moveProject.currentPackage.packageName} [${moveProject.contentRootPath}]"
    }
}

class FunctionCallConfigurationEditor<T: FunctionCallConfigurationBase>(
    private val project: Project,
    private val commandHandler: CommandConfigurationHandler,
):
    SettingsEditor<T>() {

    private var moveProject: MoveProject? = null

    // null to clear selection
    private val projectComboBox: ComboBox<MoveProjectItem> = ComboBox()
    private val accountTextField = JTextField()

    private val functionItemField = CompletionTextField(project, "", emptyList())
    private val functionApplyButton = JButton("Select and refresh UI")
    private val functionParametersPanel = FunctionParametersPanel(project, commandHandler)

    private val functionValidator: ComponentValidator

    private val rawCommandField = TextViewer("", project, true)
    private val errorLabel = JLabel("")

    private lateinit var editorPanel: DialogPanel

    init {
        errorLabel.foreground = JBColor.RED

        project.moveProjectsService.allProjects
            .forEach {
                projectComboBox.addItem(MoveProjectItem(it))
            }
        projectComboBox.isEnabled = projectComboBox.model.size > 1

        // validates
        this.functionValidator = ComponentValidator(this)
            .withValidator(Supplier<ValidationInfo?> {
                val text = functionItemField.text
                if (text.isBlank()) return@Supplier ValidationInfo("Required", functionItemField)

                val moveProject = moveProject ?: return@Supplier null

                val functionItem = commandHandler.getFunctionItem(moveProject, text)
                if (functionItem == null) {
                    return@Supplier ValidationInfo("Invalid entry function", functionItemField)
                }
                null
            })
            .andRegisterOnDocumentListener(functionItemField)
            .installOn(functionItemField)

        val editor = this
        functionParametersPanel.addFunctionCallListener(object: FunctionParameterPanelListener {
            override fun functionParametersChanged(functionCall: FunctionCall) {
                // if current project is null, this shouldn't really be doing anything, just quit
                val mp = editor.moveProject ?: return

//                editor.functionCall = functionCall
                editor.rawCommandField.text =
                    commandHandler.generateCommand(mp, functionCall, accountTextField.text).unwrapOrNull() ?: ""
            }
        })

        // enables Apply button if function name is changed and valid
        functionItemField.addDocumentListener(object: DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                // do nothing if project is null
                val mp = moveProject ?: return

                val oldFunctionName = functionParametersPanel.functionItem?.element?.qualName?.editorText()
                val newFunctionName = event.document.text
                functionApplyButton.isEnabled =
                    newFunctionName != oldFunctionName
                            && commandHandler.getFunctionItem(mp, newFunctionName) != null
            }
        })


        // update type parameters form and value parameters form on "Apply" button click
        functionApplyButton.addActionListener {
            // do nothing if project is null
            val mp = moveProject ?: return@addActionListener

            val functionItemName = functionItemField.text
            val functionItem = commandHandler.getFunctionItem(mp, functionItemName)
                ?: error("Button should be disabled if function name is invalid")

            val functionCall = FunctionCall.template(functionItem)
            functionParametersPanel.updateFromFunctionCall(functionCall)
            functionValidator.revalidate()
            functionParametersPanel.fireChangeEvent()

            functionApplyButton.isEnabled = false
        }
    }

    // called in every producer run
    override fun resetEditorFrom(commandSettings: T) {
        val workingDirectory = commandSettings.workingDirectory
        if (workingDirectory == null) {
            // if set to null, then no project was present at the time of creation, this is invalid command
            replacePanelWithErrorText("Deserialization error: no workingDirectory is present")
            return
        }

        val moveProject = project.moveProjectsService.findMoveProjectForPath(workingDirectory)
        if (moveProject == null) {
            replacePanelWithErrorText("Deserialization error: no Aptos project at the $workingDirectory")
            return
        }

        projectComboBox.selectedItem = MoveProjectItem(moveProject)

        setMoveProject(moveProject)

        val (profile, functionCall) =
            when (commandSettings.command) {
                "" -> Pair("", FunctionCall.empty())
                else -> {
                    val res = commandHandler.parseTransactionCommand(moveProject, commandSettings.command)
                    when (res) {
                        is RsResult.Ok -> res.ok
                        is RsResult.Err -> {
                            replacePanelWithErrorText("Deserialization error: ${res.err}")
                            return
                        }
                    }
                }
            }
//        this.signerAccount = profile
        this.accountTextField.text = profile
        this.functionItemField.text = functionCall.itemName() ?: ""
        functionApplyButton.isEnabled = false

        functionParametersPanel.updateFromFunctionCall(functionCall)
        functionValidator.revalidate()
    }

    override fun applyEditorTo(s: T) {
        functionParametersPanel.fireChangeEvent()
        s.command = rawCommandField.text
        s.workingDirectory = moveProject?.contentRootPath
    }

    override fun createEditor(): JComponent {
        editorPanel = createEditorPanel()
        val outerPanel = panel {
            row { cell(errorLabel) }
            row {
                cell(editorPanel)
                    .align(AlignX.FILL + AlignY.FILL)
            }
        }
        return DumbService.getInstance(project).wrapGently(outerPanel, this)
    }

    override fun disposeEditor() {
        super.disposeEditor()
        Disposer.dispose(functionParametersPanel)

    }

    fun setMoveProject(moveProject: MoveProject) {
        this.moveProject = moveProject

        functionItemField.text = ""
        accountTextField.text = ""
        functionApplyButton.isEnabled = false
        functionParametersPanel.updateFromFunctionCall(FunctionCall.empty())

        // refill completion variants
        val completionVariants = commandHandler.getFunctionCompletionVariants(moveProject)
        this.functionItemField.setVariants(completionVariants)

    }

    private fun createEditorPanel(): DialogPanel {
        val editorPanel = panel {
            row { cell(errorLabel) }
            row("Project") {
                @Suppress("UnstableApiUsage")
                cell(projectComboBox)
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .whenItemSelectedFromUi {
                        setMoveProject(it.moveProject)
                    }
            }
            row("Account") {
                cell(accountTextField)
                    .align(AlignX.FILL)
            }
            row("Entry function") {
                cell(functionItemField)
                    .align(AlignX.FILL)
                    .resizableColumn()
                cell(functionApplyButton)
            }
            separator()
            row {
                cell(functionParametersPanel)
                    .align(AlignX.FILL + AlignY.FILL)
            }
            separator()
            row("Raw") {
                cell(rawCommandField)
                    .align(AlignX.FILL)
            }
        }
        editorPanel.registerValidators(this)
        return editorPanel
    }

    private fun replacePanelWithErrorText(text: String) {
        errorLabel.text = text
        errorLabel.foreground = MessageType.ERROR.titleForeground
        errorLabel.icon = if (text.isBlank()) null else AllIcons.Actions.Lightning
        editorPanel.isVisible = false
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
}
