package org.move.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MoveFile
import org.move.lang.toMoveFile
import org.move.stdext.deepIterateChildrenRecursivery

fun deepWalkMoveFiles(project: Project, rootDir: VirtualFile, process: (MoveFile) -> Boolean) {
    deepIterateChildrenRecursivery(rootDir, { it.extension == "move" }) {
        val moveFile = it.toMoveFile(project) ?: return@deepIterateChildrenRecursivery true
        process(moveFile)
    }
}
