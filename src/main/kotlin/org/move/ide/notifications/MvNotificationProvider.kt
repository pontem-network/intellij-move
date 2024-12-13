package org.move.ide.notifications

import com.intellij.ide.impl.isTrusted
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.move.bytecode.DECOMPILED_ARTIFACTS_FOLDER
import org.move.cli.settings.MvProjectSettingsServiceBase.*
import org.move.lang.isMoveFile
import org.move.lang.isMoveTomlManifestFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isUnitTestMode
import java.util.function.Function
import javax.swing.JComponent

fun updateAllNotifications(project: Project) {
    EditorNotifications.getInstance(project).updateAllNotifications()
}

class UpdateNotificationsOnSettingsChangeListener(val project: Project): MoveSettingsListener {

    override fun <T: MvProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>) {
        updateAllNotifications(project)
    }
}

abstract class MvNotificationProvider(protected val project: Project): EditorNotificationProvider {

    protected abstract val VirtualFile.disablingKey: String

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function {
            createNotificationPanel(file, project)
        }
    }

    abstract fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel?

    protected fun disableNotification(file: VirtualFile) {
        PropertiesComponent.getInstance(project).setValue(file.disablingKey, true)
    }

    protected fun isNotificationDisabled(file: VirtualFile): Boolean =
        PropertiesComponent.getInstance(project).getBoolean(file.disablingKey)

    protected fun updateAllNotifications() {
        EditorNotifications.getInstance(project).updateAllNotifications()
    }
}

abstract class MvAptosEditorNotificationProvider(project: Project): MvNotificationProvider(project) {

    abstract val notificationProviderId: String

    abstract fun createAptosNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel?

    open val enableForScratchFiles: Boolean = false
    open val enableForDecompiledFiles: Boolean = false

    override val VirtualFile.disablingKey: String get() = notificationProviderId + path

    override fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
        // disable in unit tests
        if (isUnitTestMode) return null

        if (!enableForScratchFiles && ScratchUtil.isScratch(file)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null

        // only run for .move or Move.toml files
        if (!file.isMoveFile && !file.isMoveTomlManifestFile) return null

        val nioFile = file.toNioPathOrNull()?.toFile()
        // skip non-physical file
        if (nioFile == null) return null

        if (!enableForDecompiledFiles
            && FileUtil.startsWith(nioFile.canonicalPath, DECOMPILED_ARTIFACTS_FOLDER.canonicalPath)
        ) {
            return null
        }

        // explicitly disabled in file
        if (isNotificationDisabled(file)) return null

        return createAptosNotificationPanel(file, project)
    }

    protected fun EditorNotificationPanel.doNotShowAgainLabel(file: VirtualFile) {
        createActionLabel("Do not show again") {
            disableNotification(file)
            updateAllNotifications(project)
        }
    }
}