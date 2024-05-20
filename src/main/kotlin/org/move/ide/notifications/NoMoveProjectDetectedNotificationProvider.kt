package org.move.ide.notifications

import com.intellij.ide.impl.isTrusted
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.bytecode.AptosBytecodeDecompiler
import org.move.cli.MoveProjectsService
import org.move.cli.MoveProjectsService.Companion.MOVE_PROJECTS_TOPIC
import org.move.cli.moveProjectsService
import org.move.cli.settings.MvProjectSettingsServiceBase.*
import org.move.cli.settings.MvProjectSettingsServiceBase.Companion.MOVE_SETTINGS_TOPIC
import org.move.cli.settings.moveSettings
import org.move.lang.isMoveFile
import org.move.lang.isMoveTomlManifestFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isDispatchThread
import org.move.openapiext.common.isUnitTestMode

class NoMoveProjectDetectedNotificationProvider(project: Project): MvEditorNotificationProvider(project),
                                                                   DumbAware {

    override val VirtualFile.disablingKey: String get() = NOTIFICATION_STATUS_KEY + path

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

    override fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
        if (isUnitTestMode && !isDispatchThread) return null
        if (!(file.isMoveFile || file.isMoveTomlManifestFile)) return null
        if (ScratchUtil.isScratch(file)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null

        // check whether file is a decompiler artifact
        val decompiledArtifactsDir = AptosBytecodeDecompiler().getArtifactsDir()
        val asNioFile = file.toNioPathOrNull()?.toFile()
        if (asNioFile == null || asNioFile.relativeToOrNull(decompiledArtifactsDir) != null) return null

        val blockchain = project.moveSettings.blockchain
        val moveProjectsService = project.moveProjectsService
        // HACK: Reloads projects once on an opening of any Move file, if not yet reloaded.
        //       It should be invoked somewhere else where it's more appropriate,
        //       not in the notification handler.
        if (!moveProjectsService.initialized) {
//            moveProjectsService.scheduleProjectsRefresh(
//                reason = "called from notification on uninitialized projects service"
//            )
            // exit notification handler here, it's going to be entered again after the refresh
            return null
        }
        if (isNotificationDisabled(file)) return null

        if (moveProjectsService.allProjects.isEmpty()) {
            // no move projects available
            return EditorNotificationPanel().apply {
                text = "No $blockchain projects found"
                createActionLabel("Do not show again") {
                    disableNotification(file)
                    updateAllNotifications(project)
                }
            }
        }

        if (moveProjectsService.findMoveProjectForFile(file) == null) {
            return EditorNotificationPanel().apply {
                text = "File does not belong to any known $blockchain project"
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