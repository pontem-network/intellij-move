package org.move.stdext

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.lang3.RandomStringUtils
import org.move.lang.MoveFile
import org.move.lang.toMoveFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

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
fun Path.list(): Sequence<Path> = Files.list(this).asSequence()

fun String.pluralize(): String = StringUtil.pluralize(this)

@NlsActions.ActionText
fun String.capitalized(): String = StringUtil.capitalize(this)

fun randomLowercaseAlphabetic(length: Int): String =
    RandomStringUtils.random(length, "0123456789abcdefghijklmnopqrstuvwxyz")

fun numberSuffix(number: Int): String {
    if ((number % 100) in 11..13) {
        return "th"
    }
    return when (number % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

fun Long.isPowerOfTwo(): Boolean {
    return this > 0 && (this.and(this - 1)) == 0L
}

fun String.partitionLast(delimiter: String): Pair<String, String> {
    val head = substringBeforeLast(delimiter)
    val tail = substringAfterLast(delimiter)
    return when {
        head == tail -> head to ""
        else -> head to tail
    }
}
