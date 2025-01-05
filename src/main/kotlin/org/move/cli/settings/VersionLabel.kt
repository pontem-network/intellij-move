package org.move.cli.settings

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import org.move.cli.tools.MvCommandLine
import org.move.openapiext.UiDebouncer
import org.move.openapiext.checkIsBackgroundThread
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.execute
import org.move.openapiext.isSuccess
import java.nio.file.Path
import javax.swing.Icon

open class TextOrErrorLabel(icon: Icon?): JBLabel(icon) {

    fun isError(): Boolean = this.foreground == JBColor.RED

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

class VersionLabel(
    parentDisposable: CheckedDisposable,
    private val envs: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT,
    private val versionUpdateListener: (() -> Unit)? = null
):
    TextOrErrorLabel(null) {

    private val versionUpdateDebouncer = UiDebouncer(parentDisposable)

    fun update(execPath: Path?) {
        versionUpdateDebouncer.update(
            onPooledThread = {
                if (!isUnitTestMode) {
                    checkIsBackgroundThread()
                }
                if (execPath == null || !execPath.isValidExecutable()) {
                    return@update null
                }

                val commandLineArgs = MvCommandLine(
                    arguments = listOf("--version"),
                    workingDirectory = null,
                    environmentVariables = envs
                )
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

//    fun setText(text: String, errorHighlighting: Boolean) {
//        if (errorHighlighting) {
//            this.text = text
//            this.foreground = JBColor.RED
//        } else {
//            this.text = text
//                .split("\n")
//                .joinToString("<br>", "<html>", "</html>")
//            this.foreground = JBColor.foreground()
//        }
//    }
}
