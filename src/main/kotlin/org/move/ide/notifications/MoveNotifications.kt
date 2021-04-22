package org.move.ide.notifications

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager

object MoveNotifications {
    fun pluginNotifications(): NotificationGroup {
        return NotificationGroupManager.getInstance().getNotificationGroup("Move Plugin")
    }
}
