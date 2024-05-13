package org.move.ide.notifications

import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
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
import java.awt.Component
import java.awt.Point
import javax.swing.event.HyperlinkListener

fun Logger.logOrShowBalloon(@NotificationContent content: String) {
    when {
        isUnitTestMode -> this.warn(content)
        isDebugModeEnabled() -> showBalloonWithoutProject(content, INFORMATION)
        else -> this.debug(content)
    }
}

fun Project.showBalloon(
    @NotificationContent content: String,
    type: NotificationType,
    action: AnAction? = null
) {
    showBalloon("", content, type, action)
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
