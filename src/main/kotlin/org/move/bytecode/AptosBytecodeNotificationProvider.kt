package org.move.bytecode

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import org.move.ide.notifications.showDebugBalloon
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.openFile
import org.move.openapiext.pathAsPath
import org.move.stdext.RsResult
import org.move.stdext.unwrapOrElse
import java.util.function.Function
import javax.swing.JComponent

class AptosBytecodeNotificationProvider(project: Project): EditorNotificationProvider {

    init {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object: BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                updateAllNotifications(project)
            }
        })
    }

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        if (!FileTypeRegistry.getInstance().isFileOfType(file, AptosBytecodeFileType)) {
            return null
        }
        val properties = PropertiesComponent.getInstance(project)
        val progressManager = ProgressManager.getInstance()
        val decompilationFailedKey = DECOMPILATION_FAILED + "." + file.path

        val aptosDecompiler = AptosBytecodeDecompiler()
        val decompiledFilePath = file.parent.pathAsPath.resolve(aptosDecompiler.sourceFileName(file))
        val decompilationTask = DecompilationModalTask(project, file)

        return Function {
            EditorNotificationPanel(it).apply {
                val existingDecompiledFile =
                    VirtualFileManager.getInstance().refreshAndFindFileByNioPath(decompiledFilePath)
                if (existingDecompiledFile != null) {
                    // file exists
                    text = "Decompiled source file exists"
                    createActionLabel("Open source file") {
                        project.openFile(existingDecompiledFile)
                    }
                    return@apply
                }

                // decompiledFile does not exist
                val decompilationFailed = properties.getBoolean(decompilationFailedKey, false)
                if (decompilationFailed) {
                    text = "Decompilation command failed"
                    createActionLabel("Try again") {
                        val virtualFile = progressManager.run(decompilationTask)
                            .unwrapOrElse {
                                // something went wrong with the decompilation command again
                                project.showDebugBalloon("Error with decompilation process", it, ERROR)
                                return@createActionLabel
                            }

                        properties.setValue(decompilationFailedKey, false)
                        project.openFile(virtualFile)
                        updateAllNotifications(project)
                    }
                } else {
                    createActionLabel("Decompile into source code") {
                        val decompiledFile = progressManager.run(decompilationTask)
                            .unwrapOrElse {
                                project.showDebugBalloon("Error with decompilation process", it, ERROR)
                                return@unwrapOrElse null
                            }
                        if (decompiledFile == null) {
                            // something went wrong with the decompilation command
                            properties.setValue(decompilationFailedKey, true)
                        } else {
                            project.openFile(decompiledFile)
                        }
                        updateAllNotifications(project)
                    }
                }
            }
        }
    }

    class DecompilationModalTask(project: Project, val file: VirtualFile):
        Task.WithResult<RsResult<VirtualFile, String>, Exception>(
            project,
            "Decompiling ${file.name}...",
            true
        ) {
        override fun compute(indicator: ProgressIndicator): RsResult<VirtualFile, String> {
            val aptosDecompiler = AptosBytecodeDecompiler()
            return aptosDecompiler.decompileFileToTheSameDir(project, file)
        }
    }

    companion object {
        private const val DECOMPILATION_FAILED = "org.move.aptosDecompilerNotificationKey"

    }
}