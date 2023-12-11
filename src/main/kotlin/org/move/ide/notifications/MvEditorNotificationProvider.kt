package org.move.ide.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.move.cli.settings.MoveSettingsChangedEvent
import org.move.cli.settings.MoveSettingsListener
import java.util.function.Function
import javax.swing.JComponent

fun updateAllNotifications(project: Project) {
    EditorNotifications.getInstance(project).updateAllNotifications()
}

class UpdateNotificationsOnSettingsChangeListener(val project: Project) : MoveSettingsListener {

    override fun moveSettingsChanged(e: MoveSettingsChangedEvent) {
        updateAllNotifications(project)
    }
}

abstract class MvEditorNotificationProvider(protected val project: Project): EditorNotificationProvider {

    protected abstract val VirtualFile.disablingKey: String

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function { createNotificationPanel(file, project) }
    }

    abstract fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel?

    protected fun disableNotification(file: VirtualFile) {
        PropertiesComponent.getInstance(project).setValue(file.disablingKey, true)
    }

    protected fun isNotificationDisabled(file: VirtualFile): Boolean =
        PropertiesComponent.getInstance(project).getBoolean(file.disablingKey)

}