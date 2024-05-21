package org.move.bytecode

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.move.cli.settings.getAptosCli
import org.move.openapiext.RsProcessResult
import org.move.openapiext.pathField
import org.move.stdext.unwrapOrElse
import javax.swing.JComponent
import kotlin.io.path.Path

class FetchAptosPackageDialog(val project: Project): DialogWrapper(project, true) {

    val addressTextField = JBTextField()
    val packageTextField = JBTextField()
    val outputDirField = pathField(
        FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        this.disposable,
        "Output Directory"
    )
    val decompileCheckbox = JBCheckBox("Decompile afterwards")

    val profileField = JBTextField("default")
    val nodeApiKey = JBTextField()
    val connectionTimeout = JBTextField()

    init {
        title = "Aptos Decompiler"
        setSize(600, 400)

        outputDirField.text = project.basePath.orEmpty()
        decompileCheckbox.isSelected = true
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Address:") { cell(addressTextField).align(AlignX.FILL) }
            row("Package:") { cell(packageTextField).align(AlignX.FILL) }
            row("Output directory:") { cell(outputDirField).align(AlignX.FILL) }
            row { cell(decompileCheckbox) }

            val parametersGroup = collapsibleGroup("Connection Parameters") {
                row("Profile:") { cell(profileField) }
                row("Node API Key:") { cell(nodeApiKey) }
                row("Connection timeout:") { cell(connectionTimeout) }
            }
            parametersGroup.expanded = false

        }
    }

    override fun doOKAction() {
        val accountAddress = this.addressTextField.text
        val packageName = this.packageTextField.text
//            val profile = this.profileField.text
        val outputDir = this.outputDirField.text
        val decompile = this.decompileCheckbox.isSelected
        val aptos = project.getAptosCli(this.disposable) ?: return

        val downloadTask = object: Task.WithResult<RsProcessResult<ProcessOutput>, Exception>(
            project,
            "Downloading $accountAddress::$packageName...",
            true
        ) {
            override fun compute(indicator: ProgressIndicator): RsProcessResult<ProcessOutput> {
                return aptos.downloadPackage(project, accountAddress, packageName, outputDir,
                                             runner = { runProcessWithProgressIndicator(indicator) })
            }
        }
        ProgressManager.getInstance().run(downloadTask)
            .unwrapOrElse {
                this.setErrorText(it.message)
                return
            }

        if (decompile) {
            val decompileTask = object: Task.WithResult<RsProcessResult<ProcessOutput>, Exception>(
                project,
                "Decompiling $accountAddress::$packageName...",
                true
            ) {
                override fun compute(indicator: ProgressIndicator): RsProcessResult<ProcessOutput> {
                    val downloadedPath = Path(outputDir).resolve(packageName)
                    return aptos.decompileDownloadedPackage(downloadedPath)
                }
            }
            ProgressManager.getInstance().run(decompileTask)
                .unwrapOrElse {
                    this.setErrorText(it.message)
                    return
                }
        }

        super.doOKAction()
    }

    override fun getPreferredFocusedComponent(): JComponent = addressTextField

    override fun doValidate(): ValidationInfo? {
        return super.doValidate()
    }
}