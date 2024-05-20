package org.move.bytecode

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.toNioPathOrNull
import org.move.cli.settings.getAptosCli
import org.move.openapiext.pathAsPath
import org.move.openapiext.rootDisposable
import org.move.openapiext.rootPath
import org.move.stdext.RsResult
import org.move.stdext.unwrapOrElse
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.relativeTo

// todo: this is disabled for now, it's a process under ReadAction, and needs to be run in the indexing phase
class AptosBytecodeDecompiler: BinaryFileDecompiler {
    override fun decompile(file: VirtualFile): CharSequence {
        return file.readText()
//        val project =
//            ProjectLocator.getInstance().getProjectsForFile(file).firstOrNull { it?.isAptosConfigured == true }
//                ?: ProjectManager.getInstance().defaultProject.takeIf { it.isAptosConfigured }
//                ?: return file.readText()
//        val targetFileDir = getDecompilerTargetFileDirOnTemp(project, file) ?: return file.readText()
//        val targetFile = decompileFile(project, file, targetFileDir) ?: return file.readText()
//        return LoadTextUtil.loadText(targetFile)
    }

    fun decompileFile(project: Project, file: VirtualFile, targetFileDir: Path): RsResult<VirtualFile, String> {
        val disposable = project.createDisposableOnFileChange(file)
        val aptos = project.getAptosCli(disposable) ?: return RsResult.Err("No Aptos CLI configured")

        if (!targetFileDir.exists()) {
            targetFileDir.toFile().mkdirs()
        }

        aptos.decompileFile(file.path, outputDir = targetFileDir.toString())
            .unwrapOrElse {
                return RsResult.Err("`aptos move decompile` failed")
            }
        val decompiledName = sourceFileName(file)
        val decompiledFile =
            VirtualFileManager.getInstance().findFileByNioPath(targetFileDir.resolve(decompiledName)) ?: run {
                // something went wrong, no output file
                return RsResult.Err("Cannot find decompiled file in the target directory")
            }

        val decompiledNioFile = decompiledFile.toNioPathOrNull()?.toFile()
            ?: return RsResult.Err("Cannot convert VirtualFile to File")
        FileUtil.rename(decompiledNioFile, hashedSourceFileName(file))

        return RsResult.Ok(decompiledFile)
    }

    fun getDecompilerTargetFileDirOnTemp(project: Project, file: VirtualFile): Path? {
        val rootDecompilerDir = getArtifactsDir()
        val projectDecompilerDir = rootDecompilerDir.resolve(project.name)
        val root = project.rootPath ?: return null
        val relativeFilePath = file.parent.pathAsPath.relativeTo(root)
        val targetFileDir = projectDecompilerDir.toPath().resolve(relativeFilePath)
        return targetFileDir
    }

    fun getArtifactsDir(): File {
        return File(FileUtil.getTempDirectory(), "intellij-move-decompiled-artifacts")
    }

    fun sourceFileName(file: VirtualFile): String {
        val fileName = file.name
        return "$fileName.move"
    }

    fun hashedSourceFileName(file: VirtualFile): String {
        val fileName = file.name
        return "$fileName#decompiled.move"
    }
}

fun Project.createDisposableOnFileChange(file: VirtualFile): Disposable {
    val filePath = file.path
    val disposable = Disposer.newDisposable("Dispose on any change to the ${file.name} file")
    messageBus.connect(disposable).subscribe(
        VirtualFileManager.VFS_CHANGES,
        object: BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                if (events.any { it.path == filePath }) {
                    Disposer.dispose(disposable)
                }
            }
        }
    )
    Disposer.register(this.rootDisposable, disposable)
    return disposable
}
