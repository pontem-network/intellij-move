package org.move.bytecode

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import org.move.ide.notifications.showBalloon
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.openFile
import org.move.openapiext.toVirtualFile
import org.move.stdext.unwrapOrElse
import java.util.function.Function
import javax.swing.JComponent
import kotlin.io.path.exists

class AptosBytecodeNotificationProvider: EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        if (!FileTypeRegistry.getInstance().isFileOfType(file, AptosBytecodeFileType)) {
            return null
        }

        val aptosDecompiler = AptosBytecodeDecompiler()
        val targetFileDir = aptosDecompiler.getDecompilerTargetFileDirOnTemp(project, file)!!

        val expectedTargetFile = targetFileDir.resolve(aptosDecompiler.hashedSourceFileName(file))
        val properties = PropertiesComponent.getInstance(project)

        val triedKey = DECOMPILATION_TRIED + "-" + file.path + "-" + aptosDecompiler.hashedSourceFileName(file)
        if (!expectedTargetFile.exists()) {
            if (properties.getBoolean(triedKey, false)) {
                return Function {
                    EditorNotificationPanel(it).apply {
                        text = "Error with decompilation occurred"
                    }
                }
            }
            object : Task.Backgroundable(project, "Decompiling ${file.name}...", true) {
                override fun run(indicator: ProgressIndicator) {
                    aptosDecompiler.decompileFile(project, file, targetFileDir)
                        .unwrapOrElse {
                            project.showBalloon("Error with decompilation process", it, ERROR)
                        }
                    properties.setValue(triedKey, true)
                    updateAllNotifications(project)
                }
            }.queue()
            return null
        } else {
            return Function {
                EditorNotificationPanel(it).apply {
                    createActionLabel("Show decompiled source code") {
                        project.openFile(expectedTargetFile.toVirtualFile()!!)
                    }
                }
            }
        }
    }

    companion object {
        private const val DECOMPILATION_TRIED = "org.move.aptosDecompilerNotificationKey"
    }
}