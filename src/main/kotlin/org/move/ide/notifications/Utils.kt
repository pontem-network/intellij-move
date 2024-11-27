package org.move.ide.notifications

import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.intellij.ui.awt.RelativePoint
import org.move.cli.settings.isDebugModeEnabled
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.log
import java.awt.Component
import java.awt.Point
import javax.swing.event.HyperlinkListener

fun Logger.logOrShowBalloon(@NotificationContent content: String, productionLevel: LogLevel = LogLevel.DEBUG) {
    when {
        isUnitTestMode -> this.warn("BALLOON: $content")
        isDebugModeEnabled() -> {
            this.warn(content)
            showBalloonWithoutProject(content, INFORMATION)
        }
        else -> this.log(content, productionLevel)
    }
}

fun Logger.logOrShowBalloon(
    title: String,
    @NotificationContent content: String,
    productionLevel: LogLevel = LogLevel.DEBUG
) {
    when {
        isUnitTestMode -> this.warn("BALLOON: $title - $content")
        isDebugModeEnabled() -> {
            this.warn(content)
            showBalloonWithoutProject(title, content, INFORMATION)
        }
        else -> this.log("$title - $content", productionLevel)
    }
}

fun Project.showBalloon(
    @NotificationTitle title: String,
    @NotificationContent content: String,
    type: NotificationType,
    action: AnAction? = null,
) {
    val notification = MvNotifications.pluginNotifications().createNotification(title, content, type)
    if (action != null) {
        notification.addAction(action)
    }
    Notifications.Bus.notify(notification, this)
}

fun Project.showDebugBalloon(
    @NotificationTitle title: String,
    @NotificationContent content: String,
    type: NotificationType,
    action: AnAction? = null,
) {
    if (isDebugModeEnabled()) {
        this.showBalloon(title, content, type, action)
    }
}

fun Component.showBalloon(
    @NotificationContent content: String,
    type: MessageType,
    disposable: Disposable = ApplicationManager.getApplication(),
    listener: HyperlinkListener? = null
) {
    val popupFactory = JBPopupFactory.getInstance() ?: return
    val balloon = popupFactory.createHtmlTextBalloonBuilder(content, type, listener)
        .setShadow(false)
        .setAnimationCycle(200)
        .setHideOnLinkClick(true)
        .setDisposable(disposable)
        .createBalloon()
    balloon.setAnimationEnabled(false)
    val x: Int
    val y: Int
    val position: Balloon.Position
    if (size == null) {
        y = 0
        x = 0
        position = Balloon.Position.above
    } else {
        x = size.width / 2
        y = 0
        position = Balloon.Position.above
    }
    balloon.show(RelativePoint(this, Point(x, y)), position)
}

fun showBalloonWithoutProject(
    @NotificationContent content: String,
    type: NotificationType
) {
    val notification = MvNotifications.pluginNotifications().createNotification(content, type)
    Notifications.Bus.notify(notification)
}

fun showBalloonWithoutProject(
    title: String,
    @NotificationContent content: String,
    type: NotificationType
) {
    val notification = MvNotifications.pluginNotifications().createNotification(title, content, type)
    Notifications.Bus.notify(notification)
}
