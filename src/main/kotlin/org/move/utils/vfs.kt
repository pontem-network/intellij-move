package org.move.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.move.lang.MoveFile

fun iterateMoveFilesInFolder(
    project: Project, folder: VirtualFile, processFile: (MoveFile) -> Boolean
) {
    val psiManager = PsiManager.getInstance(project)
    VfsUtil
        .iterateChildrenRecursively(
            folder,
            { it.isDirectory || it.extension == "move" }
        ) { file ->
            if (file.isDirectory) return@iterateChildrenRecursively true
            val moveFile =
                psiManager.findFile(file) as? MoveFile
                    ?: return@iterateChildrenRecursively true
//            return@iterateChildrenRecursively true
            processFile(moveFile)
        }
}

fun getMoveFilesInFolder(project: Project, folder: VirtualFile): List<MoveFile> {
    val files = mutableListOf<MoveFile>()
    iterateMoveFilesInFolder(
        project,
        folder
    ) { file -> files.add(file); return@iterateMoveFilesInFolder true }
    return files
}
