package org.move.bytecode

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.columns
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
    val connectionTimeout = IntegerField("Connection timeout", 0, Int.MAX_VALUE)

    init {
        title = "Aptos Decompiler"
        setSize(800, 600)

        outputDirField.text = project.basePath.orEmpty()
        decompileCheckbox.isSelected = true

        connectionTimeout.setDefaultValueText("30")

        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Address:") {
                cell(addressTextField).align(AlignX.FILL)
                    .comment("Address of the account containing the package.")
            }
            row("Package name:") {
                cell(packageTextField).align(AlignX.FILL)
            }
            row("Output directory:") { cell(outputDirField).align(AlignX.FILL) }
            row { cell(decompileCheckbox) }

            val parametersGroup =
                collapsibleGroup("Connection Parameters") {
                    row("Aptos profile:") {
                        cell(profileField).columns(12)
                            .comment(
                                "Profile to use from the CLI config. " +
                                        "This will be used to override associated settings " +
                                        "such as the REST URL, the Faucet URL, and the private key arguments."
                            )
                    }
                    row("Node API Key:") {
                        cell(nodeApiKey).align(AlignX.FILL)
                            .comment(
                                "Key to use for ratelimiting purposes with the node API. " +
                                        "This value will be used as `Authorization: Bearer <key>`. " +
                                        "You may also set this with the NODE_API_KEY environment variable"
                            )
                    }
                    row("Connection timeout:") {
                        cell(connectionTimeout).columns(8)
                            .comment("Connection timeout in seconds, used for the REST endpoint of the fullnode")
                    }
                }
            parametersGroup.expanded = false

        }
    }

    override fun doOKAction() {
        val accountAddress = this.addressTextField.text
        val packageName = this.packageTextField.text
        val outputDir = this.outputDirField.text
        val decompile = this.decompileCheckbox.isSelected

        val aptos = project.getAptosCli(this.disposable) ?: return

        val aptosProfile = this.profileField.text
        val nodeApiKey = this.nodeApiKey.text
        val connectionTimeout = this.connectionTimeout.value

        val downloadTask = object: Task.WithResult<RsProcessResult<ProcessOutput>, Exception>(
            project,
            "Downloading $accountAddress::$packageName...",
            true
        ) {
            override fun compute(indicator: ProgressIndicator): RsProcessResult<ProcessOutput> {
                return aptos.downloadPackage(project, accountAddress, packageName, outputDir,
                                             profile = aptosProfile,
                                             connectionTimeoutSecs = connectionTimeout,
                                             nodeApiKey = nodeApiKey,
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
}