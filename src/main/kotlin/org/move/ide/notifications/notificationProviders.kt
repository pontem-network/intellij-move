package org.move.ide.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.move.cli.CommandLineExecutable
import org.move.lang.isMoveFile
import org.move.openapiext.common.isUnitTestMode
import org.move.settings.MoveSettingsChangedEvent
import org.move.settings.MoveSettingsListener
import org.move.settings.dovePath
import org.move.settings.moveSettings

fun updateAllNotifications(project: Project) {
    EditorNotifications.getInstance(project).updateAllNotifications()
}

class UpdateNotificationsOnSettingsChangeListener(val project: Project) : MoveSettingsListener {

    override fun moveSettingsChanged(e: MoveSettingsChangedEvent) {
        updateAllNotifications(project)
    }

}

class InvalidDoveExecutableNotificationsProvider(
    private val project: Project
) : EditorNotifications.Provider<EditorNotificationPanel>(),
    DumbAware {

    val VirtualFile.disablingKey: String
        get() = NOTIFICATION_STATUS_KEY + path

    override fun getKey(): Key<EditorNotificationPanel> = PROVIDER_KEY

    protected fun disableNotification(file: VirtualFile) {
        PropertiesComponent.getInstance(project).setValue(file.disablingKey, true)
    }

    protected fun isNotificationDisabled(file: VirtualFile): Boolean =
        PropertiesComponent.getInstance(project).getBoolean(file.disablingKey)

    override fun createNotificationPanel(
        file: VirtualFile,
        fileEditor: FileEditor,
        project: Project
    ): EditorNotificationPanel? {
        if (isUnitTestMode) return null
        if (!file.isMoveFile || isNotificationDisabled(file)) return null

        val doveExecutablePath = project.dovePath ?: return null
        if (CommandLineExecutable(project, doveExecutablePath).version() != null) return null

        return EditorNotificationPanel().apply {
            text = "Dove configured incorrectly"
            createActionLabel("Configure") {
                project.moveSettings.showMoveConfigureSettings()
            }
            createActionLabel("Do not show again") {
                disableNotification(file)
                updateAllNotifications(project)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_STATUS_KEY = "org.move.hideDoveNotifications"

        private val PROVIDER_KEY: Key<EditorNotificationPanel> = Key.create("Fix Dove.toml file")
    }
}
