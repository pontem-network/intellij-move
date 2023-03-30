package org.move.cli.runConfigurations.aptos.run

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import org.move.lang.core.psi.MvElement
import org.move.lang.core.types.ItemFQName
import org.move.lang.index.MvEntryFunctionIndex
import org.move.utils.ui.MoveTextFieldWithCompletion
import org.move.utils.ui.WorkingDirectoryField
import javax.swing.JComponent
import javax.swing.JLabel

class RunCommandConfigurationEditor(
    val project: Project,
    private var command: String
) : SettingsEditor<RunCommandConfiguration>() {

    private var transaction: Transaction? = null

    private val commandTextField = EditorTextField("")
    private val workingDirectoryField = WorkingDirectoryField()
    private val environmentVariablesField = EnvironmentVariablesComponent()

    private val typeParametersLabel = JLabel("")
    private val parametersLabel = JLabel("")


    init {
        commandTextField.isViewer = true
    }

    private val functionIdField: TextFieldWithAutoCompletion<String> =
        textFieldWithCompletion(
            transaction?.functionId?.cmdText() ?: "",
            MvEntryFunctionIndex.getAllKeysForCompletion(project)
        )

    override fun applyEditorTo(s: RunCommandConfiguration) {
        s.command = command
    }

    override fun resetEditorFrom(s: RunCommandConfiguration) {
        this.command = s.command
    }

    override fun createEditor(): JComponent {
        val editorPanel = createEditorPanel()
        return DumbService.getInstance(project).wrapGently(editorPanel, project)
    }

    private fun createEditorPanel(): JComponent {
        val parsedTransaction = Transaction.parseFromCommand(project, command)
        transaction = parsedTransaction

        functionIdField.text = parsedTransaction?.functionId?.cmdText() ?: ""
        refreshLabels()

        lateinit var editorPanel: DialogPanel
        editorPanel = panel {
            row("Command (generated):") {
                cell(commandTextField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
            }
            separator()
            row("Function:") {
                cell(functionIdField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                comment("(required)")
            }
            row {
                // TODO: disable button if previous function is not modified
                button("Change Function") {
                    val functionIdText = functionIdField.text.trim()
                    val itemFQName = ItemFQName.fromCmdText(functionIdText)
                    if (itemFQName != null) {
                        val function =
                            MvEntryFunctionIndex.getFunction(project, itemFQName.cmdText())
                        if (function != null) {
                            transaction = Transaction.template(function)
                            refreshLabels()
                            return@button
                        }
                    }
                    // TODO: show error and don't apply the change
                    transaction = null
                    refreshLabels()
                }
            }
            separator()
            row("Type Parameters:") { cell(typeParametersLabel) }
            row("Parameters:") { cell(parametersLabel) }
            row {
                // TODO: add popup if function id is not selected or invalid
                button("Edit Parameters") {
                    val fqName = transaction?.functionId?.cmdText() ?: return@button
                    val entryFunction = MvEntryFunctionIndex.getFunction(project, fqName)
                    if (entryFunction == null) return@button

                    val transaction = transaction ?: return@button

                    // TODO: button inactive if no params required
                    val parametersDialog = TransactionParametersDialog(
                        entryFunction,
                        transaction,
                    )
                    val ok = parametersDialog.showAndGet()
                    if (!ok) return@button

                    refreshLabels()
                }
            }
            // TODO: validation
            separator()
            row(workingDirectoryField.label) {
                cell(workingDirectoryField)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
            row("Profile") {
                textField()
                    .horizontalAlign(HorizontalAlign.FILL)
            }
            row(environmentVariablesField.label) {
                cell(environmentVariablesField)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
        editorPanel.registerValidators(this.project)
        return editorPanel
    }

    fun refreshLabels() {
        val commandText = transaction?.commandText() ?: ""
        command = commandText

        commandTextField.text = commandText
        typeParametersLabel.text = transaction?.typeParams?.map { "${it.key}=" }?.joinToString(", ") ?: "null"
        parametersLabel.text = transaction?.params?.map { "${it.key}=" }?.joinToString(", ") ?: "null"
    }

//    private fun validateFunctionId(): ValidationInfoBuilder.(TextFieldWithAutoCompletion<String>) -> ValidationInfo? {
//        return {
//            if (!MvEntryFunctionIndex.hasFunction(project, it.text)) {
//                functionId = null
//                error("Invalid entry function")
//            } else {
//                functionIdText = it.text
//                null
//            }
//        }
//    }


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

    private fun qualPathFieldWithCompletion(
        element: MvElement,
        variants: Collection<String>
    ): LanguageTextField {
        val project = element.project
        // TODO: add TYPE icon
        val completionProvider = TextFieldWithAutoCompletion.StringsCompletionProvider(variants, null)
        return MoveTextFieldWithCompletion(
            project,
            "",
            completionProvider,
            element,
        )
    }
}


fun EditorTextField.enteredTextSatisfies(predicate: (String) -> Boolean): ComponentPredicate {
    return EditorFieldPredicate(this, predicate)
}

private class EditorFieldPredicate(
    private val component: EditorTextField,
    private val predicate: (String) -> Boolean
) : ComponentPredicate() {
    override fun invoke(): Boolean = predicate(component.text)

    override fun addListener(listener: (Boolean) -> Unit) {
        component.document.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    listener(invoke())
                }
            }
        )
    }
}
