package org.move.cli.runConfigurations.buildtool

import com.intellij.build.BuildContentManager
import com.intellij.ide.nls.NlsMessages
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.SystemNotifications
import org.move.ide.notifications.MvNotifications

object AptosBuildManager {
    fun showBuildNotification(
        project: Project,
        messageType: MessageType,
        @NlsContexts.SystemNotificationTitle message: String,
        @NlsContexts.SystemNotificationText details: String? = null,
        time: Long = 0
    ) {
        val notificationContent = buildNotificationMessage(message, details, time)
        val notification = MvNotifications.buildLogGroup().createNotification(notificationContent, messageType)
        notification.notify(project)

        if (messageType === MessageType.ERROR) {
            val manager = ToolWindowManager.getInstance(project)
            invokeLater {
                manager.notifyByBalloon(BuildContentManager.TOOL_WINDOW_ID, messageType, notificationContent)
            }
        }

        SystemNotifications.getInstance().notify(
            notification.groupId,
            StringUtil.capitalizeWords(message, true),
            details ?: ""
        )
    }

    @NlsContexts.NotificationContent
    private fun buildNotificationMessage(message: String, details: String?, time: Long): String {
        var notificationContent = "$message ${details.orEmpty()}"
        if (time > 0) notificationContent += "\\ in ${NlsMessages.formatDuration(time)}"
        return notificationContent
    }
}