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
import org.move.cli.settings.getEndlessCli
import org.move.cli.settings.isEndlessConfigured
import org.move.openapiext.RsProcessResult
import org.move.openapiext.pathField
import org.move.stdext.blankToNull
import org.move.stdext.unwrapOrElse
import javax.swing.JComponent
import kotlin.io.path.Path

class FetchEndlessPackageDialog(val project: Project): DialogWrapper(project, true) {

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
    val networkUrl = JBTextField()
    val connectionTimeout = IntegerField("Connection timeout", 0, Int.MAX_VALUE)

    init {
        title = "Endless Decompiler"
        setSize(800, 600)

        outputDirField.text = project.basePath.orEmpty()
        decompileCheckbox.isSelected = true

        connectionTimeout.defaultValue = -1
        connectionTimeout.setDefaultValueText("30")

        init()

        if (!project.isEndlessConfigured) {
            setErrorText("Endless CLI is not provided in the plugin settings")
            okAction.isEnabled = false
        }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Address:") {
                cell(addressTextField).align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isBlank()) {
                            return@validationOnApply error("Cannot be empty")
                        }
                        null
                    }
                    .comment("Address of the account containing the package.")
            }
            row("Package name:") {
                cell(packageTextField).align(AlignX.FILL)
                    .validationOnApply {
                        if (it.text.isBlank()) {
                            return@validationOnApply error("Cannot be empty")
                        }
                        null
                    }
            }
            row("Output directory:") { cell(outputDirField).align(AlignX.FILL) }
            row { cell(decompileCheckbox) }

            val parametersGroup =
                collapsibleGroup("Connection Parameters") {
                    row("Endless profile:") {
                        cell(profileField).columns(12)
                            .comment(
                                "Profile to use from the CLI config. " +
                                        "This will be used to override associated settings " +
                                        "such as the REST URL, the Faucet URL, and the private key arguments."
                            )
                    }
                    row("Network URL:") {
                        cell(networkUrl).align(AlignX.FILL)
                            .comment(
                                "URL to a fullnode on the network. Leaving it blank defaults to the URL in the `default` profile."
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

        // cannot be null, it's checked at time of window creation
        val endless = project.getEndlessCli(this.disposable) ?: return

        val endlessProfile = this.profileField.text.blankToNull()
        val nodeApiKey = this.nodeApiKey.text.blankToNull()
        val networkUrl = this.networkUrl.text.blankToNull()
        val connectionTimeout = this.connectionTimeout.value

        if (okAction.isEnabled) {
            applyFields()
        }

        val downloadTask = object: Task.WithResult<RsProcessResult<ProcessOutput>, Exception>(
            project,
            "Downloading $accountAddress::$packageName...",
            true
        ) {
            override fun compute(indicator: ProgressIndicator): RsProcessResult<ProcessOutput> {
                return endless.downloadPackage(project, accountAddress, packageName, outputDir,
                                             profile = endlessProfile,
                                             networkUrl = networkUrl,
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
                    return endless.decompileDownloadedPackage(downloadedPath)
                }
            }
            ProgressManager.getInstance().run(decompileTask)
                .unwrapOrElse {
                    this.setErrorText(it.message)
                    return
                }
        }

        close(OK_EXIT_CODE)
    }

    override fun getPreferredFocusedComponent(): JComponent = addressTextField
}