package org.move.stdext

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory

private val LOG = Logger.getInstance("#org.move.stdext")

fun String.toPath(): Path = Paths.get(this)

fun String.toPathOrNull(): Path? = pathOrNull(this::toPath)

fun Path.resolveOrNull(other: String): Path? = pathOrNull { resolve(other) }

private inline fun pathOrNull(block: () -> Path): Path? {
    return try {
        block()
    } catch (e: InvalidPathException) {
        LOG.warn(e)
        null
    }
}

fun Path.isExecutableFile(): Boolean {
    return !this.isDirectory() && Files.isExecutable(this)
}

fun Path.exists(): Boolean = Files.exists(this)

fun executableName(toolName: String): String =
    if (SystemInfo.isWindows) "$toolName.exe" else toolName

fun String.blankToNull(): String? = ifBlank { null }