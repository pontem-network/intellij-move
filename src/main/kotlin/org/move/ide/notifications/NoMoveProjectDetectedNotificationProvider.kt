package org.move.ide.notifications

import com.intellij.ide.impl.isTrusted
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.moveProjects
import org.move.lang.isMoveFile
import org.move.lang.isMoveTomlManifestFile
import org.move.openapiext.common.isDispatchThread
import org.move.openapiext.common.isUnitTestMode

class NoMoveProjectDetectedNotificationProvider(project: Project): MvEditorNotificationProvider(project),
                                                                   DumbAware {

    override val VirtualFile.disablingKey: String get() = NOTIFICATION_STATUS_KEY + path

    override fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
        if (isUnitTestMode && !isDispatchThread) return null
        if (!(file.isMoveFile || file.isMoveTomlManifestFile)) return null
        if (ScratchUtil.isScratch(file)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null
        if (isNotificationDisabled(file)) return null

        val moveProjects = project.moveProjects
        if (!moveProjects.initialized) return null

        if (moveProjects.allProjects.isNotEmpty()) {
            // no move projects available
            return EditorNotificationPanel().apply {
                text = "No Aptos projects found"
                createActionLabel("Do not show again") {
                    disableNotification(file)
                    updateAllNotifications(project)
                }
            }
        }

        if (moveProjects.findMoveProjectForFile(file) == null) {
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

        const val NO_MOVE_PROJECTS = "NoMoveProjects"
        const val FILE_NOT_IN_MOVE_PROJECT = "FileNotInMoveProject"
    }
}