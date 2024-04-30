package org.move.ide.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import org.move.cli.download.AptosInstaller
import org.move.cli.download.AptosItem
import org.move.cli.settings.TextOrErrorLabel
import org.move.cli.settings.isValidExecutable
import org.move.stdext.exists
import java.nio.file.Files
import javax.swing.JComponent
import kotlin.io.path.name

class DownloadAptosDialog(val project: Project?): DialogWrapper(project, true) {

    private val cliListModel = CollectionComboBoxModel<AptosItem>()
    private val clisComboBox = ComboBox(cliListModel)

    val versionTextField = JBTextField()
    val errorLabel = TextOrErrorLabel(UIUtil.getErrorIcon())

    init {
        title = "Download Aptos"

        val installDir = AptosInstaller.installDir
        val regex = Regex("""aptos-cli-(\d+.\d+.\d+)""")
        if (installDir.exists()) {
            for (aptosFile in Files.walk(installDir, 0)) {
                if (aptosFile.isValidExecutable()) {
                    val match = regex.find(aptosFile.name)
                    if (match != null) {
                        val (version) = match.destructured
                        cliListModel.add(AptosItem(version))
                    }
                }
            }
        }

        if (cliListModel.selectedItem == null && cliListModel.size != 0) {
            cliListModel.selectedItem = cliListModel.items.first()
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row { cell(clisComboBox) }
            row("Version") {
                cell(versionTextField).align(AlignX.FILL).columns(10)
                cell(errorLabel)
            }
            row {
                button("Download") {
                    // TODO: validate format
                    val version = versionTextField.text
                    ProgressManager.getInstance()
                        .run(object: Task.WithResult<Unit, Exception>(project, title, true) {
                            override fun compute(indicator: ProgressIndicator) {
                                service<AptosInstaller>()
                                    .installAptosCli(
                                        AptosItem(version), indicator,
                                        onFinish = { aptosItem ->
                                            val prevSize = cliListModel.size
                                            cliListModel.add(aptosItem)
                                            if (prevSize == 0) {
                                                cliListModel.selectedItem = aptosItem
                                            }
                                        })
                            }
                        })
                }
            }
        }
    }
}

class DownloadAptosAction: DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val dialog = DownloadAptosDialog(e.project)
        dialog.showAndGet()
    }
}