package org.move.ide.notifications

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.MoveProjectsService
import org.move.cli.MoveProjectsService.Companion.MOVE_PROJECTS_TOPIC
import org.move.cli.moveProjectsService
import org.move.cli.settings.MvProjectSettingsServiceBase.*
import org.move.cli.settings.MvProjectSettingsServiceBase.Companion.MOVE_SETTINGS_TOPIC

class NoAptosProjectDetectedNotification(project: Project): MvAptosEditorNotificationProvider(project),
                                                            DumbAware {

    init {
        project.messageBus.connect().apply {
            subscribe(MOVE_SETTINGS_TOPIC, object: MoveSettingsListener {
                override fun <T: MvProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>) {
                    updateAllNotifications()
                }
            })

            subscribe(MOVE_PROJECTS_TOPIC, MoveProjectsService.MoveProjectsListener { _, _ ->
                updateAllNotifications()
            })
        }
    }

    override val notificationProviderId: String get() = NOTIFICATION_STATUS_KEY

    override fun createAptosNotificationPanel(
        file: VirtualFile,
        project: Project
    ): EditorNotificationPanel? {

        val moveProjectsService = project.moveProjectsService
        // HACK: Reloads projects once on an opening of any Move file, if not yet reloaded.
        //       It should be invoked somewhere else where it's more appropriate,
        //       not in the notification handler.
        if (!moveProjectsService.initialized) {
            // exit notification handler here, it's going to be entered again after the refresh
            return null
        }

        if (moveProjectsService.allProjects.isEmpty()) {
            // no move projects available
            return EditorNotificationPanel().apply {
                text = "No Aptos projects found"
                createActionLabel("Do not show again") {
                    disableNotification(file)
                    updateAllNotifications(project)
                }
            }
        }

        if (moveProjectsService.findMoveProjectForFile(file) == null) {
            return EditorNotificationPanel().apply {
                text = "File does not belong to any known Aptos project"
                createActionLabel("Do not show again") {
                    disableNotification(file)
                    updateAllNotifications(project)
                }
            }
        }

        return null
    }

    companion object {
        private const val NOTIFICATION_STATUS_KEY = "org.move.hideNoMoveProjectNotifications"
    }
}