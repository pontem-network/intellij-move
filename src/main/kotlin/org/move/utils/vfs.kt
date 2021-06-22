package org.move.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.move.lang.MoveFile

fun iterateOverMoveFiles(project: Project, folder: VirtualFile, processFile: (MoveFile) -> Boolean) {
    val psiManager = PsiManager.getInstance(project)
    VfsUtil.iterateChildrenRecursively(
        folder,
        { it.isDirectory || it.extension == "move" }
    ) { file ->
        if (file.isDirectory) return@iterateChildrenRecursively true
        val moveFile =
            psiManager.findFile(file) as? MoveFile
                ?: return@iterateChildrenRecursively true
        processFile(moveFile)
    }
}
