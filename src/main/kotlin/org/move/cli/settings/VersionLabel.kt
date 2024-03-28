package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import org.move.cli.runConfigurations.CliCommandLineArgs
import org.move.openapiext.UiDebouncer
import org.move.openapiext.checkIsBackgroundThread
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.execute
import org.move.openapiext.isSuccess
import java.nio.file.Path
import javax.swing.JLabel

class VersionLabel(
    parentDisposable: Disposable,
    private val versionUpdateListener: (() -> Unit)? = null
) : JLabel() {

    private val versionUpdateDebouncer = UiDebouncer(parentDisposable)

    fun updateAndNotifyListeners(execPath: Path?) {
        versionUpdateDebouncer.update(
            onPooledThread = {
                if (!isUnitTestMode) {
                    checkIsBackgroundThread()
                }
                if (execPath == null || !execPath.isValidExecutable()) {
                    return@update null
                }

                val commandLineArgs =
                    CliCommandLineArgs(null, listOf("--version"), workingDirectory = null)
                commandLineArgs
                    .toGeneralCommandLine(execPath)
                    .execute()
            },
            onUiThread = { versionCmdOutput ->
                if (versionCmdOutput == null) {
                    setTextInvalidExecutable()
//                    this.setText("N/A (Invalid executable)", errorHighlighting = true)
                } else {
                    if (versionCmdOutput.isSuccess) {
                        val versionText = versionCmdOutput.stdoutLines.joinToString("\n")
                        this.setText(versionText, errorHighlighting = false)
                    } else {
                        this.setText(
                            "N/A (Cannot run --version command. Error code is ${versionCmdOutput.exitCode})",
                            errorHighlighting = true
                        )
                    }
                }
                versionUpdateListener?.invoke()
            }
        )
    }

    fun setTextInvalidExecutable() = this.setText("N/A (Invalid executable)", errorHighlighting = true)

    fun setText(text: String, errorHighlighting: Boolean) {
        if (errorHighlighting) {
            this.text = text
            this.foreground = JBColor.RED
        } else {
            this.text = text
                .split("\n")
                .joinToString("<br>", "<html>", "</html>")
            this.foreground = JBColor.foreground()
        }
    }
}
