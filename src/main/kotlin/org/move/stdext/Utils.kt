package org.move.stdext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MoveFile
import org.move.lang.toMoveFile

/**
 * Just a way to force exhaustiveness analysis for Kotlin's `when` expression.
 *
 * Use it like this:
 * ```
 * when (foo) {
 *     is Bar -> {}
 *     is Baz -> {}
 * }.exhaustive // ensure `Bar` and `Baz` are the only variants of `foo`
 * ```
 */
val <T> T.exhaustive: T
    inline get() = this

fun deepIterateChildrenRecursivery(
    root: VirtualFile,
    filePredicate: (VirtualFile) -> Boolean,
    processDirectory: (VirtualFile) -> Boolean = { true },
    processFile: (VirtualFile) -> Boolean,
) {
    VfsUtil.iterateChildrenRecursively(root, { it.isDirectory || filePredicate(it) }) {
        if (it.isDirectory) return@iterateChildrenRecursively processDirectory(it)
        processFile(it)
    }
}

fun VirtualFile.iterateFiles(
    filePredicate: (VirtualFile) -> Boolean,
    processDirectory: (VirtualFile) -> Boolean = { true },
    processFile: (VirtualFile) -> Boolean
) {
    return deepIterateChildrenRecursivery(this, filePredicate, processDirectory, processFile)
}

fun VirtualFile.iterateMoveVirtualFiles(
    process: (VirtualFile) -> Boolean
) {
    return this.iterateFiles({ it.extension == "move" }, processFile = process)
}

fun VirtualFile.iterateMoveFiles(
    project: Project,
    process: (MoveFile) -> Boolean
) {
    return this.iterateFiles({ it.extension == "move" }) {
        val moveFile = it.toMoveFile(project) ?: return@iterateFiles true
        process(moveFile)
    }
}
