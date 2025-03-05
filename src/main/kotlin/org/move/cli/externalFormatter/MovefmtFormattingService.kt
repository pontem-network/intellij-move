package org.move.cli.externalFormatter

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.psi.formatter.FormatterUtil
import org.move.cli.externalFormatter.MovefmtFormattingService.Companion.FormattingReason.*
import org.move.ide.notifications.showBalloon
import org.move.lang.MoveFile
import org.move.openapiext.rootPath
import org.move.openapiext.showSettingsDialog
import org.move.stdext.blankToNull
import org.move.stdext.emptyEnumSet
import org.move.stdext.unwrapOrThrow

class MovefmtFormattingService: AsyncDocumentFormattingService() {
    // only whole file formatting is supported
    override fun getFeatures(): Set<FormattingService.Feature> = emptyEnumSet()

    override fun canFormat(file: PsiFile): Boolean =
        file is MoveFile && file.project.movefmtSettings.useMovefmt && getFormattingReason() == ReformatCode

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        val context = request.context
        val project = context.project
        val settings = project.movefmtSettings

        val disposable = Disposer.newDisposable()
        val movefmt = project.getMovefmt(disposable)
        if (movefmt == null) {
            project.showBalloon(MOVEFMT_ERROR,
                                "movefmt executable configured incorrectly",
                                ERROR,
                                object : DumbAwareAction("Edit movefmt settings") {
                                    override fun actionPerformed(e: AnActionEvent) {
                                        e.project?.showSettingsDialog<MovefmtConfigurable>()
                                    }
                                }
            )
            return null
        }

        val projectDirectory = project.rootPath ?: return null
        val fileOnDisk = request.ioFile ?: return null

        return object: FormattingTask {
            private val indicator: ProgressIndicatorBase = ProgressIndicatorBase()

            override fun run() {
                val arguments = settings.additionalArguments.blankToNull()?.split(" ").orEmpty()
                val envs = EnvironmentVariablesData.create(settings.envs, true)
                movefmt.reformatFile(
                    fileOnDisk,
                    additionalArguments = arguments,
                    workingDirectory = projectDirectory,
                    envs,
                    runner = {
                        addProcessListener(object: CapturingProcessAdapter() {
                            override fun processTerminated(event: ProcessEvent) {
                                val exitCode = event.exitCode
                                if (exitCode == 0) {
                                    val filteredStdout = filterBuggyLines(output.stdout)
                                    request.onTextReady(filteredStdout)
                                } else {
                                    request.onError("Movefmt", output.stderr)
                                }
                            }
                        })
                        runProcessWithProgressIndicator(indicator)
                    }
                )
                    .unwrapOrThrow()
            }

            override fun cancel(): Boolean {
                indicator.cancel()
                disposable.dispose()
                return true
            }

            override fun isRunUnderProgress(): Boolean = true
        }
    }

    private fun filterBuggyLines(stdout: String): String {
        return stdout.lines()
            .takeWhile { !it.contains("files successfully formatted") }
            .joinToString("\n")
    }

    override fun getNotificationGroupId(): String = "Move Language"

    override fun getName(): String = "movefmt"

    companion object {
        private const val MOVEFMT_ERROR = "movefmt error"

        private enum class FormattingReason {
            ReformatCode,
            ReformatCodeBeforeCommit,
            Implicit
        }

        private fun getFormattingReason(): FormattingReason =
            when (CommandProcessor.getInstance().currentCommandName) {
                ReformatCodeProcessor.getCommandName() -> ReformatCode
                FormatterUtil.getReformatBeforeCommitCommandName() -> ReformatCodeBeforeCommit
                else -> Implicit
            }
    }
}