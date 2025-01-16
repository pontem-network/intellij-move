package org.move.cli.sentryReporter

import com.intellij.diagnostic.DiagnosticBundle
import com.intellij.diagnostic.IdeErrorsDialog
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.UserFeedback
import io.sentry.protocol.Message
import io.sentry.protocol.SentryId
import org.move.cli.settings.moveSettings
import org.move.openapiext.project
import org.move.stdext.asMap
import java.awt.Component


class SentryErrorReporter: ErrorReportSubmitter() {
    init {
        Sentry.init { options ->
            options.dsn = "https://a3153f348f8d43f189c4228db47cfc0d@sentry.pontem.network/6"
            options.isEnableUncaughtExceptionHandler = false
        }
    }

    override fun getReportActionText(): String = "Report to Pontem Network"

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val project = parentComponent.project

        object: Task.Backgroundable(project, "Sending error report", false) {
            override fun run(indicator: ProgressIndicator) {
                val mainEvent = events[0]
                val sentryEvent = createSentryEventFromError(project, mainEvent)
                try {
                    // io
                    val sentryEventId = Sentry.captureEvent(sentryEvent)
                    if (successfullySent(sentryEventId)) {
                        if (additionalInfo != null) {
                            val userFeedback = UserFeedback(sentryEventId)
                            userFeedback.comments = additionalInfo
                            // io
                            Sentry.captureUserFeedback(userFeedback)
                        }
                        onSuccess(project, consumer::consume)
                    }
                } catch (e: Exception) {
                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
                }
            }
        }.queue()

        return true
    }

    private fun createSentryEventFromError(project: Project?, event: IdeaLoggingEvent): SentryEvent {
        val sentryEvent = SentryEvent()
        sentryEvent.level = SentryLevel.ERROR

        val plugin = this.pluginDescriptor

        val pluginInfoContext = mutableMapOf<String, Any>()
        pluginInfoContext["Platform"] = ApplicationInfo.getInstance().fullApplicationName
        pluginInfoContext["Plugin Version"] = plugin?.version ?: "unknown"
        sentryEvent.contexts["Plugin Info"] = pluginInfoContext
//        try {
//        } catch (e: NoSuchFieldError) {
//            // intellij 2023.1 on windows 11 throws this, catch and report that one instead
//            // TODO: remove later
//            sentryEvent.contexts["Runtime Error Stacktrace"] = mapOf("Value" to e.getThrowableText())
//        }
//

        if (project != null) {
            val settings = project.moveSettings.state.asMap().toMutableMap()
            settings.remove("aptosPath")
            sentryEvent.contexts["Settings"] = settings
            // TODO: serialization doesn't work for some reason
//            sentryEvent.contexts["Projects"] =
//                project.moveProjectsService.allProjects.map { MoveProjectContext.from(it) }.toList()
        }
        // IdeaReportingEvent only provides text-based stacktrace, no way to convert into SentryException
        sentryEvent.contexts["Stacktrace"] = mapOf("Value" to event.throwableText)

        val sentryMessage = Message()
        sentryMessage.formatted = event.errorMessage
        sentryEvent.message = sentryMessage

        sentryEvent.fingerprints = listOf("{{ default }}", event.errorMessage)

        // add pluginId to environment to filter out sui plugin
        sentryEvent.environment = "production:${plugin?.pluginId ?: "unknownId"}"

        return sentryEvent
    }

    private fun successfullySent(sentryEventId: SentryId): Boolean {
        return sentryEventId != SentryId.EMPTY_ID
    }
}

private val IdeaLoggingEvent.errorMessage: String? get() = throwableText.split('\n').getOrNull(0)

private fun onSuccess(project: Project?, callback: Consumer<in SubmittedReportInfo>) {
    val reportInfo = SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
    callback.consume(reportInfo)
    ApplicationManager.getApplication().invokeLater {
        val title = DiagnosticBundle.message("error.report.submitted")
        val content = DiagnosticBundle.message("error.report.gratitude")
        NotificationGroupManager.getInstance().getNotificationGroup("Error Report")
            .createNotification(title, content, NotificationType.INFORMATION)
            .setImportant(false)
            .notify(project)
    }
}
