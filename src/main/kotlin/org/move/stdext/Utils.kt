package org.move.stdext

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Files
import java.nio.file.Path

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
